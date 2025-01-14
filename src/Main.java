import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main{

    public static final boolean DEBUG_PRINT = true;
    final static Scanner scan = new Scanner(System.in);

    public static void main(String[] args) {

        System.out.println("Which project are you trying to view?");
        System.out.println("1. Lexical Analyzer (LexAn.java)");
        System.out.println("2. LR Algorithm Syntax Parser (LRParser.java)");

        switch(scan.nextLine()){
            case "1" -> Project1();
            case "2" -> Project2();
        }
    }

    public static void Project1(){
        LexAn test = null;
        do {
            System.out.print("Enter file name: ");
            try {
                test = LexAn.fromFile(scan.nextLine());
            } catch (FileNotFoundException e) {
                System.out.println("File not found!");
            }
        } while (test == null);
        do{
            test.lex();

            System.out.println("Line " + test.getLineNumber() + " Next token: " + test.getTokenCode() + "-" + test.getTokenName() + ", Next Lexeme: " + test.lexemeToString());
        }while(!test.isFinished());
    }

    public static void Project2(){
        LRParser parser = new LRParser();
        LRParser.TreeNode outputParseTree;
        String input;

        System.out.println("Supported Tokens:");
        System.out.println(parser.getImplementedTokens());

        System.out.println("\nLanguage Rules:");
        System.out.println(parser.getRules());

        for(;;){
            System.out.print("\nEnter a string to parse (or '/exit' to exit): ");
            input = scan.nextLine();
            if(input.equals("/exit")) break;

            parser.loadString(input);

            outputParseTree = parser.parse();

            System.out.println("Resulting Parse Tree (read top to bottom):");
            System.out.println(outputParseTree.toString());
        }
    }
}