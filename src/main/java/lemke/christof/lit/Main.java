package lemke.christof.lit;

import lemke.christof.lit.commands.CommitCommand;
import lemke.christof.lit.commands.InitCommand;
import lemke.christof.lit.commands.TestCommand;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        System.out.println("start");
        if (args.length < 1) {
            System.err.println("Missing command");
            System.exit(1);
        }
        String commadString = args[0];
        Path pwd = Path.of("").toAbsolutePath();
        Workspace ws = new Workspace(pwd);
        Database db = new Database(pwd);
        Environment env = new Environment();
        switch (commadString) {
            case "commit":
                new CommitCommand(ws, db, env).run();
                break;
            case "init":
                new InitCommand(args).run();
                break;
            case "test":
                new TestCommand().run();
                break;
            default:
                System.err.println("Unknown command: "+commadString);
                System.exit(1);
        }
    }
}