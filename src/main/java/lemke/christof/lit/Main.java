package lemke.christof.lit;

import lemke.christof.lit.commands.*;

import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        Path pwd = Path.of("").toAbsolutePath();
        Repository repo = Repository.create(pwd);
        try {
            if (args.length < 1) {
                throw new RuntimeException("Missing command");
            }
            Command cmd = createCommand(args[0], repo);
            String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);
            cmd.run(remainingArgs);
        } catch (Exception e) {
            repo.io().err().println(e.getMessage());
            System.exit(1);
        }
    }

    private static Command createCommand(String command, Repository repo) {
        return switch (command) {
            case "commit" -> new CommitCommand(repo);
            case "init" -> new InitCommand();
            case "test" -> new TestCommand();
            case "add" -> new AddCommand(repo);
            case "status" -> new StatusCommand(repo, System.console() != null);
            case "diff" -> new DiffCommand(repo, System.console() != null);
            case "branch" -> new BranchCommand(repo);
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