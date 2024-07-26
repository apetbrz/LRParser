import java.io.FileNotFoundException;
import java.util.*;

public class LRParser {

    //helper records/enum:

    //Lexeme: for storing lexemes (token and string value)
    //(good for identifier names, primarily)
    public record Lexeme(LexAn.TOKEN token, String value){
        public String toString(){
            return value;
        }
    }

    //Rule: for defining grammar rules
    //LHS- nonterminal result, RHS- term/nonterm string as an Object[]
    public record Rule(Object LHS, Object[] RHS){
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append(LHS);
            sb.append(" -> ");
            for(Object o : RHS){
                sb.append(o);
                sb.append(" ");
            }
            return sb.toString();
        }
    }

    //TreeNode: for building a parse tree
    public record TreeNode(Object data, Object[] children){
        /*
        print algorithm taken from https://stackoverflow.com/a/8948691 and modified
        i just wanted a fancy tree display :P not my algorithm
         */

        public String toString(){
            StringBuilder buffer = new StringBuilder(100);
            print(buffer, this, "", "");
            return buffer.toString();
        }

        private static void print(StringBuilder buffer, Object node, String prefix, String childrenPrefix){
            if(node == null){
                buffer.append("[null]");
                return;
            }
            buffer.append(prefix);
            if(node instanceof Lexeme){
                buffer.append("[");
                buffer.append(((Lexeme)node).value);
                buffer.append("]\n");
                return;
            }

            buffer.append(((TreeNode)node).data);
            buffer.append("\n");

            Object[] kids = ((TreeNode)node).children;
            for(int i = 0; i < kids.length; i++){
                Object next = kids[i];
                if(i == kids.length - 1) print(buffer, next, childrenPrefix + "└── ", childrenPrefix + "    ");
                else print(buffer, next, childrenPrefix + "├── ", childrenPrefix + "│   ");
            }

        }
    }

    //NONTERMINAL: every nonterminal defined in the grammar
    enum NONTERMINAL{
        EXPR,
        TERM,
        FACT;

        @Override
        public String toString() {
            return name();
        }
    }

    //ACTION_TABLE and GOTO:
    //the LR Parsing table, stored as two enums of table columns
    //each of these has a lookup() function, which takes a TOKEN and returns the correct column
    //empty table elements are left as null values
    //TODO: WHY DID I DO ENUMS? REPLACE WITH ARRAYS? EASIER TO INTEGRATE WITH TABLE GENERATORS

    //ACTION_TABLE: each value has a TOKEN (terminal) representing the column label
    //and a String array (actions) representing each row of the column
    private enum ACTION_TABLE {
        ID(LexAn.TOKEN.IDENTIFIER, new String[]{
                "s5",null,null,null,"s5",null,"s5","s5"
        }),
        ADD_OP(LexAn.TOKEN.ADD_OP, new String[]{
                null,"s6","r2","r4",null,"r6",null,null,"s6","r1","r3","r5"
        }),
        MULT_OP(LexAn.TOKEN.MULT_OP, new String[]{
                null,null,"s7","r4",null,"r6",null,null,null,"s7","r3","r5"
        }),
        LEFT_PAREN(LexAn.TOKEN.LEFT_PAREN, new String[]{
                "s4",null,null,null,"s4",null,"s4","s4",null,null,null,null
        }),
        RIGHT_PAREN(LexAn.TOKEN.RIGHT_PAREN, new String[]{
                null,null,"r2","r4",null,"r6",null,null,"s11","r1","r3","r5"
        }),
        END_STR(LexAn.TOKEN.EOF, new String[]{
                null,"acc","r2","r4",null,"r6",null,null,null,"r1","r3","r5"
        });

        public final LexAn.TOKEN terminal;
        public final String[] actions;

        ACTION_TABLE(LexAn.TOKEN terminal, String[] actions) {
            this.terminal = terminal;
            this.actions = Arrays.copyOf(actions, 12);//guarantees length 12 array

        }

        public static ACTION_TABLE lookup(LexAn.TOKEN token){
            for(ACTION_TABLE a : ACTION_TABLE.values()){
                if(a.terminal == token) return a;
            }
            return null;
        }
    }

    //GOTO_TABLE: each value has a NONTERMINAL (label) representing the column label
    //and an Integer array (states) representing each row of the column
    private enum GOTO_TABLE {
        E(NONTERMINAL.EXPR, new Integer[]{
                1,null,null,null,8
        }),
        T(NONTERMINAL.TERM, new Integer[]{
                2,null,null,null,2,null,9
        }),
        F(NONTERMINAL.FACT, new Integer[]{
                3,null,null,null,3,null,3,10
        });

        public final NONTERMINAL label;
        public final Integer[] states;

        GOTO_TABLE(NONTERMINAL label, Integer[] states) {
            this.label = label;
            this.states = Arrays.copyOf(states,12);
        }

        public static GOTO_TABLE lookup(NONTERMINAL nonterm){
            for(GOTO_TABLE g : GOTO_TABLE.values()){
                if(g.label == nonterm) return g;
            }
            return null;
        }

    }

    //LANGUAGE_RULES: an array of Rules defining the language grammar.
    //each Reduce action in the LR algorithm points to an index of this
    static final Rule[] LANGUAGE_RULES = {
            new Rule(NONTERMINAL.EXPR, new Object[]{NONTERMINAL.EXPR, LexAn.TOKEN.ADD_OP, NONTERMINAL.TERM}),
            new Rule(NONTERMINAL.EXPR, new Object[]{NONTERMINAL.TERM}),
            new Rule(NONTERMINAL.TERM, new Object[]{NONTERMINAL.TERM, LexAn.TOKEN.MULT_OP, NONTERMINAL.FACT}),
            new Rule(NONTERMINAL.TERM, new Object[]{NONTERMINAL.FACT}),
            new Rule(NONTERMINAL.FACT, new Object[]{LexAn.TOKEN.LEFT_PAREN, NONTERMINAL.EXPR, LexAn.TOKEN.RIGHT_PAREN}),
            new Rule(NONTERMINAL.FACT, new Object[]{LexAn.TOKEN.IDENTIFIER})
    };

    //PDAStack: the main stack used by the LR algorithm
    Stack<Object> PDAStack;
    //lexical: the lexical analyzer used for analyzing terminals from the input
    private final LexAn lexical;

    //Default Constructor: creates lexical object with no input
    public LRParser(){
        lexical = new LexAn();
    }

    //loadFile(): loads a file from the root directory into the lexical analyzer
    public void loadFile(String fileName) throws FileNotFoundException {
        lexical.loadFile(fileName);
    }
    //loadString(): loads a text string directly into the lexical analyzer
    public void loadString(String input){
        lexical.loadString(input);
    }
    //initializeStack(): create a new Stack object and push the initial state of 0 onto it
    public void initializeStack(){
        PDAStack = new Stack<>();
        PDAStack.push(0);
    }

    //parse(): perform the LR Parsing algorithm
    public TreeNode parse(){

        //initialize the stack
        initializeStack();

        //initialize all temporary variables
        LexAn.TOKEN currentToken = null;    //currentToken: the token at the front of the input stream
        ACTION_TABLE tokenColumn = null;    //tokenColumn: the ACTION_TABLE column of the current token
        GOTO_TABLE gotoColumn = null;       //gotoColumn: the GOTO_TABLE column of the LHS of a rule from a Reduce step
        String currentAction = null;        //currentAction: the tokenColumn row for the current state
        int nextState = 0;                  //nextState: the next state value to go onto the stack after each step
        Rule reduceRule = null;             //reduceRule: the grammar rule to follow for the current Reduce operation
        Lexeme currentLexeme = null;         //currentLexeme: the lexeme object that will be pushed onto the stack/tree

        ArrayList<Object> treeNodeStack = new ArrayList<>();    //treeNodeStack: used to store parse tree nodes while parsing
        //to construct a full parse tree, i use an ArrayList to store leaves/nodes temporarily
        //technically this is not used as a stack, since i access data outside of only popping
        //but for the most part, im only adding things or removing things from the end, just in different orders, so i called it a stack anyways

        //call for the first lexeme, storing it in lexical
        lexical.lex();

        //begin parse loop
        do{
            //grab the token of the current lexeme
            currentToken = lexical.getToken();

            //get its ACTION_TABLE column
            tokenColumn = ACTION_TABLE.lookup(currentToken);

            //the column should NOT be null
            if(tokenColumn == null) throw new RuntimeException("\nTOKEN MISSING FROM ACTION_TABLE: " + currentToken.name());

            //the current state is the one at the top of the stack, so grab the appropriate action from the ACTION_TABLE column at that row
            currentAction = tokenColumn.actions[(Integer)PDAStack.peek()];

            //if the action is null, we landed on a blank space, meaning there is a syntax error!!!
            if(currentAction == null) throw new RuntimeException("\nSYNTAX ERROR AT LINE " + lexical.getLineNumber() + " COL " + (lexical.getColNumber()-1));

            //developer info, prints out the stack and the next step
            if(Main.DEBUG_PRINT) {
                System.out.println(PDAStack);
                System.out.println("NEXT ACTION: " + currentAction);
            }

            //based on the first character of the current action, we do different things
            switch(currentAction.charAt(0)){

                //if the action starts with 's', it is a Shift action, pushing the next terminal onto the stack
                case 's':
                    //create the lexeme object
                    currentLexeme = new Lexeme(currentToken, lexical.lexemeToString());

                    //push the lexeme onto the stack
                    PDAStack.push(currentLexeme);

                    //add the lexeme to the end of the list of tree nodes
                    treeNodeStack.add(currentLexeme);

                    //parse the next state from the action (chars after the first)
                    nextState = Integer.parseInt(currentAction.substring(1));

                    //push that onto the stack
                    PDAStack.push(nextState);

                    //get the next lexeme from the string, for the next step
                    lexical.lex();

                    //exit switch
                    break;

                //if the action starts with 'r', it is a Reduce action, converting a RHS to a LHS using the Rules defined by LANGUAGE_RULES
                case 'r':
                    //the rule to be used is the one in the given action from the table (chars after the first)
                    reduceRule = LANGUAGE_RULES[Integer.parseInt(currentAction.substring(1))-1];

                    //for each grammar symbol in the handle being removed,
                    for(int i = 0; i < reduceRule.RHS.length; i++){
                        //pop two elements from the stack (symbol + state number)
                        PDAStack.pop();
                        PDAStack.pop();
                    }

                    //afterwards, record the exposed state number to be used in the GOTO table
                    nextState = (Integer)PDAStack.peek();

                    //push the LHS of the grammar rule onto the stack
                    PDAStack.push(reduceRule.LHS);

                    //grab the appropriate GOTO column (from the new nonterminal just pushed to the stack)
                    gotoColumn = GOTO_TABLE.lookup((NONTERMINAL)PDAStack.peek());

                    //it should not be null!
                    if(gotoColumn == null) throw new RuntimeException("\nNONTERMINAL MISSING FROM GOTO_TABLE: " + PDAStack.peek());

                    //and grab the next state to go to from the GOTO table (from the state under the new LHS on the stack)
                    nextState = gotoColumn.states[nextState];

                    //and push this new state onto the stack
                    PDAStack.push(nextState);

                    //finally, handle the tree-related funny business

                    //create a new array for child nodes of the new tree node, with size equal to the reduceRule's RHS
                    Object[] newChildren = new Object[reduceRule.RHS.length];

                    //then, iterate through the last n treeNodeStack nodes, where n is the size of the reduceRule's RHS
                    for(int i = treeNodeStack.size() - reduceRule.RHS.length, j = 0; i < treeNodeStack.size(); i++, j++){
                        //add each of these nodes to the new array
                        newChildren[j] = treeNodeStack.get(i);
                        //and remove from the "stack", and decrement i by one as well, to match the changing of treeNodeStack.size()
                        treeNodeStack.remove(i--);
                    }
                    //finally add a new tree node with this new array as its children to the "stack"
                    treeNodeStack.add(new TreeNode(reduceRule.LHS, newChildren));

                    //here's an example to visually explain ^this^ block of code:
                    //for treeNodeStack = [w,x,y,z] where w,x,y,z are leaf nodes, and reduceRule.RHS.length = 3,
                    //after this code is ran, treeNodeStack == [w,A] where A is a new node with [x,y,z] as children
                    //thus, this serves to construct a full TreeNode parse tree as the LR algorithm progresses

                    //exit switch
                    break;

                //finally, if the action starts with 'a', it must be "acc", or the Accept action, meaning the input is parsed!!! yay!!!!
                case 'a':
                    System.out.println("DONE!!!!!!!!!!");
                    break;

            }//end currentAction switch

        }while(currentAction != "acc"); //loop so long as the currentAction is not the Accept action

        //and finally, return
        return (TreeNode) treeNodeStack.get(0);
    }//end parse()

    //getRules(): returns a string containing all the grammar rules of the language
    public String getRules(){
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for(Rule r : LANGUAGE_RULES){
            sb.append(i++);
            sb.append(". ");
            sb.append(r);
            sb.append("\n");
        }
        return sb.substring(0, sb.length()-1);
    }
    //getImplementedTokens(): returns a string listing every terminal token in the ACTION_TABLE tree
    public String getImplementedTokens(){
        StringBuilder sb = new StringBuilder();
        for(ACTION_TABLE col : ACTION_TABLE.values()){
            sb.append(col.terminal);
            sb.append(", ");
        }
        return sb.substring(0, sb.length()-2);
    }

}