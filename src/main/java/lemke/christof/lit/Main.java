package lemke.christof.lit;

import lemke.christof.lit.commands.AddCommand;
import lemke.christof.lit.commands.CommitCommand;
import lemke.christof.lit.commands.InitCommand;
import lemke.christof.lit.commands.TestCommand;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Missing command");
            System.exit(1);
        }
        Path pwd = Path.of("").toAbsolutePath();
        Workspace ws = new Workspace(pwd);
        Database db = new Database(pwd);
        Index idx = new Index(pwd);
        Environment env = new Environment();
        Refs refs = new Refs(pwd);
        switch (args[0]) {
            case "commit":
                new CommitCommand(ws, db, env, refs).run();
                break;
            case "init":
                new InitCommand(args).run();
                break;
            case "test":
                new TestCommand().run();
                break;
            case "add":
                new AddCommand(ws, db, idx, args).run();
                break;
            default:
                System.err.println("Unknown command: "+ args[0]);
                System.exit(1);
        }
    }
}