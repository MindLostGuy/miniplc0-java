package miniplc0java;

import miniplc0java.error.CompileError;
import org.junit.Test;
import static org.junit.Assert.*;

public class AnalyserTest {
    public static void main(String[] args) throws CompileError {
        String[] strings = {"-o", "output.txt", "hello.txt", "-t"};
        App.main(strings);
    }

}