import java.io.*;
import java.util.regex.Pattern;

public class LexAn {

    /*
     * LexAn.java:  my lexical analyzer
     *              char and token codes taken from textbook
     */

    //char classes:
    enum CHAR_CLASS{
        EOF,
        LETTER,
        DIGIT,
        QUOTE,              //<--- excessive?
        DECIMAL_SEPARATOR,  //<--- excessive?
        UNKNOWN
    }

    //lexeme tokens:
    enum TOKEN{
        ERROR,
        EOF,
        KEYWORD,
        INT_LIT,
        DBL_LIT,
        STR_LIT,
        IDENTIFIER,
        ASSIGN_OP,
        ADD_OP,
        SUB_OP,
        MULT_OP,
        DIV_OP,
        ILLEGAL_OP,
        LEFT_PAREN,
        RIGHT_PAREN,
        COMMA,
        SEMICOLON
    }

    //keywords:
    enum KEYWORD{
        KEYWORD_INT("int"),
        KEYWORD_DOUBLE("double"),
        KEYWORD_STRING("String");
        public final String id;
        KEYWORD(String id){
            this.id = id;
        }
    }

    //important regex strings:
    static final Pattern INTEGER_LITERAL_REGEX = Pattern.compile("[0-9]+");
    static final Pattern DOUBLE_LITERAL_REGEX = Pattern.compile("[0-9]+\\.[0-9]+");

    //global vars
    static CHAR_CLASS charClass;
    static StringBuilder lexeme;
    static char nextChar;
    static TOKEN token;
    static int lineNumber = 1;
    static int colNumber = 0;

    //file input
    static BufferedReader reader;


    //default constructor: initialize array
    public LexAn(){
        lexeme = new StringBuilder(100);
    }
    //private BufferedReader constructor, to be used by factory methods
    private LexAn(BufferedReader newReader){
        this();

        //set reader from factory method
        reader = newReader;

        //get first char
        getChar();
    }

    //fromFile(): builds a LexAn for a specific file name
    public static LexAn fromFile(String fileName) throws FileNotFoundException {
        return new LexAn(new BufferedReader(new FileReader(fileName)));
    }
    //loadFile(): for loading a different file into memory
    public void loadFile(String fileName) throws FileNotFoundException {
        //start over
        clearLexeme();

        //get file
        reader = new BufferedReader(new FileReader(fileName));

        //get first char
        getChar();
    }
    //loadString(): for loading an input string into memory
    public void loadString(String input){
        //start over
        clearLexeme();

        //get input
        reader = new BufferedReader(new StringReader(input));

        getChar();
    }


    //lex(): fetch next lexeme, store it in lexeme[] and return its token.
    public TOKEN lex(){
        //if at end of file, fail
        if(isFinished()){
            System.out.println("ERROR: END OF FILE! do not call pls");
        }

        //clear previous data
        clearLexeme();

        //get next non-blank character
        getNonBlank();

        //act differently based on which class of character is found
        switch(charClass){
            //if a letter, assume identifier
            case LETTER:
                //add/get characters so long as they are letters or digits
                do{
                    addChar();
                    getChar();
                }while(charClass == CHAR_CLASS.LETTER || charClass == CHAR_CLASS.DIGIT);
                //check string to valid keywords
                KEYWORD key = lookupKeyword(); //just in case unique keyword token IDs matter later, this function returns the keyword type
                //if keyword is valid, the string is a keyword, otherwise its an identifier
                token = key != null ? TOKEN.KEYWORD : TOKEN.IDENTIFIER;
                //exit switch
                break;

            //if a digit, assume an integer
            case DIGIT:
                //add/get characters so long as they are digits (or a decimal separator)
                do{
                    addChar();
                    getChar();
                }while(charClass == CHAR_CLASS.DIGIT || charClass == CHAR_CLASS.DECIMAL_SEPARATOR);
                //check for integer
                if(isIntegerLiteral()) token = TOKEN.INT_LIT;
                //check for double
                else if(isDoubleLiteral()) token = TOKEN.DBL_LIT;
                //otherwise, too many decimal points, error
                else token = TOKEN.ERROR;

                //exit switch
                break;

            //if a quotation mark, assume a string literal
            case QUOTE:
                //add/get characters so long as they are valid characters
                do {
                    addChar();
                    getChar();
                    //should only exit string literal on end of line, end of quote, or end of file
                }while(nextChar != '\n' && charClass != CHAR_CLASS.EOF && charClass != CHAR_CLASS.QUOTE);
                //check if next char is a quote. if not, its an error
                if(charClass == CHAR_CLASS.QUOTE){
                    //add the quote, output token
                    addChar();
                    token = TOKEN.STR_LIT;
                    getChar();  //continue
                }
                else token = TOKEN.ERROR;

                break;

            //if not recognized as letter or digit, look up
            case UNKNOWN:
                //save token as the looked up one
                token = lookupOp(nextChar);
                //continue
                getChar();
                //exit switch
                break;

            //if end of file, store as end of file.
            case EOF:
                token = TOKEN.EOF;
                lexeme.append("EOF");
                break;

            default:
                addChar();
                token = TOKEN.ERROR;
                getChar();
        }

        //output token
        return token;
    }

    //getChar(): fetch next character in file. place char into nextChar, place character class into charClass
    private void getChar(){
        try{
            nextChar = (char) reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        colNumber++;
        if(nextChar == '\n'){
            lineNumber++;
            colNumber = 0;
        }

        if(nextChar == (char)-1) {
            charClass = CHAR_CLASS.EOF;
        }
        else if(nextChar == '$'){       //TODO: REMOVE? ONLY USED FOR PROJECT 2 BUT KINDA POINTLESS NORMALLY
            charClass = CHAR_CLASS.EOF;
        }
        else if(Character.isLetter(nextChar)){
            charClass = CHAR_CLASS.LETTER;
        }
        else if(Character.isDigit(nextChar)){
            charClass = CHAR_CLASS.DIGIT;
        }
        else if(nextChar == '"') {
            charClass = CHAR_CLASS.QUOTE;
        }
        else if(nextChar == '.') {
            charClass = CHAR_CLASS.DECIMAL_SEPARATOR;
        }
        else{
            charClass = CHAR_CLASS.UNKNOWN;
        }
    }
    //addChar(): insert character into lexeme array. display error if lexeme too long
    private void addChar(){
        lexeme.append(nextChar);
    }
    //getNonBlank(): call getChar() until it fetches a non-whitespace character
    //includes the CURRENT character, so if used for a check, make sure to getChar() at the end of it!!!
    private void getNonBlank(){
        while(Character.isWhitespace(nextChar)){
            getChar();
        }
    }

    //lookupOp(): check char for special characters (operators, etc) and return it TODO: LOOKUP TABLE INSTEAD OF SWITCH?
    private TOKEN lookupOp(char ch){
        addChar();
        return switch(ch){
            case '=' -> TOKEN.ASSIGN_OP;
            case '+' -> TOKEN.ADD_OP;
            case '-' -> TOKEN.SUB_OP;
            case '*' -> TOKEN.MULT_OP;
            case '/' -> TOKEN.DIV_OP;
            case '(' -> TOKEN.LEFT_PAREN;
            case ')' -> TOKEN.RIGHT_PAREN;
            case ',' -> TOKEN.COMMA;
            case ';' -> TOKEN.SEMICOLON;
            default -> TOKEN.ILLEGAL_OP;
        };
    }
    //lookupKeyword(): check lexeme string for special identifiers/keywords and return the keyword type, or null if not found
    private KEYWORD lookupKeyword(){
        String lex = lexemeToString();
        for(KEYWORD k : KEYWORD.values()){
            if(k.id.equals(lex)){
                return k;
            }
        }
        return null;
    }

    //isStringLiteral(): check lexeme string for a string literal
    private boolean isIntegerLiteral(){
        return INTEGER_LITERAL_REGEX.matcher(lexemeToString()).matches();
    }
    //isDoubleLiteral(): check lexeme string for a double literal
    private boolean isDoubleLiteral(){
        return DOUBLE_LITERAL_REGEX.matcher(lexemeToString()).matches();
    }

    //clearLexeme(): wipe lexeme
    private void clearLexeme(){
        lexeme.delete(0, lexeme.length());
        token = null;
    }


    //getters:
    public String lexemeToString(){
        return lexeme.toString();
    }
    public int getTokenCode(){
        return token.ordinal();
    }
    public String getTokenName(){
        return token.name();
    }
    public TOKEN getToken(){
        return token;
    }
    public int getLineNumber(){
        return lineNumber;
    }
    public int getColNumber() {
        return colNumber;
    }

    //isFinished(): returns true if at end of file, false otherwise
    public boolean isFinished(){
        return token == TOKEN.EOF || reader == null;
    }
}