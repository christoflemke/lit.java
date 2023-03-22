package lemke.christof.lit;

import java.io.IOException;
import java.nio.file.*;
import java.util.regex.Pattern;

public record Refs (Path root) {
    Path headPath() {
        return gitPath().resolve("HEAD");
    }

    private Path gitPath() {
        return root.resolve(".git");
    }

    private Path refsPath() {
        return gitPath().resolve("refs");
    }

    private Path headsPath() {
        return refsPath().resolve("heads");
    }

    public void updateHead(String ref) {

        try {
            Path tempFile = Files.createTempFile(gitPath(), "head-", null);
            Files.writeString(tempFile, ref);
            Files.move(tempFile, headPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String readHeadBranch() {
        String ref;
        try {
            ref = Files.readString(headPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (ref.startsWith("ref:")) {
            String[] split = ref.split(" ");
            String[] path = split[1].split("/");
            return path[path.length - 1].trim();
        } else {
            return ref.trim();
        }
    }

    public String readHead() {
        try {
            String ref = Files.readString(headPath());
            if (ref.startsWith("ref:")) {
                String[] split = ref.split(" ");
                String sha = Files.readString(gitPath().resolve(Path.of(split[1].trim())));
                return sha.trim();
            }
            return ref.trim();
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createBranch(String branchName) {
        String ref = readHead();
        writeRef(branchName, ref);
    }

    private void writeRef(String branchName, String ref) {
        try {
            validateBranchName(branchName);
            Path branchPath = gitPath().resolve("refs").resolve("heads").resolve(branchName);
            Files.createDirectories(branchPath.getParent());
            Path tmpPath = branchPath.getParent().resolve(branchName + ".tmp");
            Files.writeString(tmpPath, ref + "\n");
            try {
                Files.move(tmpPath, branchPath);
            }
            catch (FileAlreadyExistsException e) {
                throw new RuntimeException("Branch already exists: " + branchName);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * - any path component of it begins with ".", or
     * - it has double dots "..", or
     * - it has ASCII control characters, or
     * - it has ":", "?", "[", "\", "^", "~", SP, or TAB anywhere, or
     * - it has "*" anywhere unless REFNAME_REFSPEC_PATTERN is set, or
     * - it ends with a "/", or
     * - it ends with ".lock", or
     * - it contains a "@{" portion
     */
    private static final Pattern INVALID_BRANCH_NAME = Pattern.compile("""
            ^\\.| # begins with "."
            \\.\\.| # includes ".."
            [\\x00-\\x20] # includes control characters
            [:?\\[^~\\s]| # includes ":", "?", "[", "\", "^", "~", SP, or TAB]
            /$| # ends with "/"
            \\.lock$| # ends with ".lock"
            @\\{ # contains "@{"
            """, Pattern.COMMENTS);
    private static void validateBranchName(String branchName) {
        if (INVALID_BRANCH_NAME.matcher(branchName).find()) {
            throw new RuntimeException("Invalid branch name: " + branchName);
        }
    }
}
