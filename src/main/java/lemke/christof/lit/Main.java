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
        return switch (command) {
            case "commit" -> new CommitCommand(repo);
            case "init" -> new InitCommand();
            case "test" -> new TestCommand();
            case "add" -> new AddCommand(repo);
            case "status" -> new StatusCommand(repo, System.console() != null);
            case "show_head" -> new ShowHeadCommand(repo);
            case "list_head" -> new ListHeadCommand(repo);
            default -> {
                System.err.println("Unknown command: " + command);
                System.exit(1);
                yield null;
            }
        };
    }
}