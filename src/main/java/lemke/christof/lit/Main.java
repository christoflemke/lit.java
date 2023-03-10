package lemke.christof.lit;

import lemke.christof.lit.commands.*;

import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Missing command");
            System.exit(1);
        }
        Path pwd = Path.of("").toAbsolutePath();
        Repository repo = Repository.create(pwd);
        Command cmd = createCommand(args[0], repo);
        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
        cmd.run(remainingArgs);
    }

    private static Command createCommand(String command, Repository repo) {
        switch (command) {
            case "commit":
                return new CommitCommand(repo);
            case "init":
                return new InitCommand();
            case "test":
                return new TestCommand();
            case "add":
                return new AddCommand(repo);
            case "status":
                return new StatusCommand(repo);
            case "show_head":
                return new ShowHeadCommand(repo);
            default:
                System.err.println("Unknown command: " + command);
                System.exit(1);
                return null;
        }
    }
}