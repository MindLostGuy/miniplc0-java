package miniplc0java;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import miniplc0java.analyser.Analyser;
import miniplc0java.error.CompileError;
import miniplc0java.instruction.Instruction;
import miniplc0java.tokenizer.StringIter;
import miniplc0java.tokenizer.Token;
import miniplc0java.tokenizer.TokenType;
import miniplc0java.tokenizer.Tokenizer;

import net.sourceforge.argparse4j.*;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class App {
    public static void main(String[] args) throws CompileError{
        var argparse = buildArgparse();
        Namespace result;
        try {
            result = argparse.parseArgs(args);
        } catch (ArgumentParserException e1) {
            argparse.handleError(e1);
            return;
        }

        var inputFileName = result.getString("input");
        var outputFileName = result.getString("output");

        InputStream input;
        if (inputFileName.equals("-")) {
            input = System.in;
        } else {
            try {
                input = new FileInputStream(inputFileName);
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find input file.");
                e.printStackTrace();
                System.exit(2);
                return;
            }
        }

        PrintStream output;
        if (outputFileName.equals("-")) {
            output = System.out;
        } else {
            try {
                output = new PrintStream(new FileOutputStream(outputFileName));
            } catch (FileNotFoundException e) {
                System.err.println("Cannot open output file.");
                e.printStackTrace();
                System.exit(2);
                return;
            }
        }

        Scanner scanner;
        scanner = new Scanner(input);
        var iter = new StringIter(scanner);
        var tokenizer = tokenize(iter);

        if (result.getBoolean("tokenize")) {
            // tokenize
            var tokens = new ArrayList<Token>();
            try {
                while (true) {
                    var token = tokenizer.nextToken();
                    if (token.getTokenType().equals(TokenType.EOF)) {
                        break;
                    }
                    tokens.add(token);
                }
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                for(StackTraceElement s:e.getStackTrace()){
                    System.err.println(s);
                }
                System.exit(-1);
                return;
            }
            for (Token token : tokens) {
                output.println(token.toString());
            }
        } else if (result.getBoolean("analyse")) {
//            while (scanner.hasNextLine()){
//                String str = scanner.nextLine();
//                System.out.println(str);
//            }
//            System.out.println("!@#!#!#!");
            // analyze
            var analyzer = new Analyser(tokenizer);
            List<Instruction> instructions;
            try {
                instructions = analyzer.analyse();
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                output.close();
                scanner.close();
                System.err.println(e);
                for(StackTraceElement s:e.getStackTrace()){
                    System.err.println(s);
                }
                System.exit(-1);
                return;
            }
            try {
                //byte[] tmp = new byte[]{114, 48, 59, 62, 0, 0, 0, 1, 0, 0, 0, 2, 1, 0, 0, 0, 6, 95, 115, 116, 97, 114, 116, 1, 0, 0, 0, 4, 109, 97, 105, 110, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 72, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 33, 10, 0, 0, 0, 0, 80, 23, 10, 0, 0, 0, 0, 19, 1, 0, 0, 0, 0, 0, 0, 0, 0, 48, 58, 66, 0, 0, 0, 23, 10, 0, 0, 0, 1, 80, 23, 10, 0, 0, 0, 2, 80, 23, 10, 0, 0, 0, 1, 19, 84, 1, 0, 0, 0, 0, 0, 0, 0, 10, 85, 10, 0, 0, 0, 2, 19, 84, 1, 0, 0, 0, 0, 0, 0, 0, 10, 85, 10, 0, 0, 0, 0, 10, 0, 0, 0, 0, 19, 1, 0, 0, 0, 0, 0, 0, 0, 1, 33, 23, 65, -1, -1, -1, -29, 73};
                //output.write(tmp);
                output.write(analyzer.program.toBytes());
                output.close();
                scanner.close();
            } catch (Exception e) {
                // 遇到错误不输出，直接退出
                output.close();
                scanner.close();
                System.err.println(e.getStackTrace());
                System.exit(-1);
                return;
            }
            /*
//            for (Instruction instruction : instructions) {
//                //output.println(instruction.toString());
//                //output.format("%-20s %s\n",instruction.toByteString() , instruction.toString());
//
//                List<Byte> bytes = new ArrayList<Byte>();
//                bytes.add((byte) 20);
//                byte[] bytes1 = {1,2,3};
//                try{
//                    output.writeBytes(bytes1);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }*/
        } else {
            System.err.println("Please specify either '--analyse' or '--tokenize'.");
            System.exit(3);
        }
    }

    private static ArgumentParser buildArgparse() {
        var builder = ArgumentParsers.newFor("miniplc0-java");
        var parser = builder.build();
        parser.addArgument("-t", "--tokenize").help("Tokenize the input").action(Arguments.storeTrue());
        parser.addArgument("-l", "--analyse").help("Analyze the input").action(Arguments.storeTrue());
        parser.addArgument("-o", "--output").help("Set the output file").required(true).dest("output")
                .action(Arguments.store());
        parser.addArgument("file").required(true).dest("input").action(Arguments.store()).help("Input file");
        return parser;
    }

    private static Tokenizer tokenize(StringIter iter) {
        var tokenizer = new Tokenizer(iter);
        return tokenizer;
    }
}
