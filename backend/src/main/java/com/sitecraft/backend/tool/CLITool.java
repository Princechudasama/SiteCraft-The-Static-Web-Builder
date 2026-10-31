package com.sitecraft.backend.tool;

import jakarta.annotation.PostConstruct;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class CLITool {

    @Value("${app.workspace.root}")
    private String workspaceRoot;

    private Path rootPath;

    private static final long MAX_FILE_SIZE = 1024 * 1024;

    private static final String[] ALLOWED_EXTENSIONS = {
            ".html", ".css", ".js"
    };

    @PostConstruct
    public void initialize() throws IOException {
        Path root = Paths.get(workspaceRoot).toAbsolutePath().normalize();

        Files.createDirectories(root);

        rootPath = root.toRealPath();
    }

    // --------------------------------------------------
    // 1. CREATE DIRECTORY
    // --------------------------------------------------

    @Tool(
            name = "create_directory",
            description = """
            Create a new directory inside the permitted website workspace.

            Use this tool when you need to organize website files into
            folders such as css, js, images, assets, or components.

            Provide a relative directory path.
            Do not provide an absolute path.
            """
    )
    public String createDirectory(

            @ToolParam(description = """
                Relative directory path to create.
                Example: portfolio/css or portfolio/js
                """)
            String directoryPath

    ) {

        try {

            Path path = resolveSafePath(directoryPath);

            Files.createDirectories(path);

            return "Directory created successfully: " + directoryPath;

        } catch (Exception e) {

            return "Failed to create directory: " + e.getMessage();

        }
    }


    // --------------------------------------------------
    // 2. CREATE FILE
    // --------------------------------------------------

    @Tool(
            name = "create_file",
            description = """
            Create a new HTML, CSS, or JavaScript file inside the workspace.

            Use this tool to create website files such as:
            index.html, style.css, script.js.

            You can optionally provide initial file content.
            The tool does not overwrite an existing file.
            Create the parent directory first if necessary.
            """
    )
    public String createFile(

            @ToolParam(description = """
                Relative file path including filename and extension.
                Example: portfolio/index.html
                """)
            String filePath,

            @ToolParam(description = """
                Initial content of the file.
                Provide an empty string if the file should initially be empty.
                """)
            String content

    ) {

        try {

            Path path = resolveSafePath(filePath);

            validateFileExtension(path);

            if (Files.exists(path)) {
                return "File already exists: " + filePath;
            }

            if (!Files.isDirectory(path.getParent())) {
                return "Parent directory does not exist. Create it first.";
            }

            if (content.length() > MAX_FILE_SIZE) {
                return "File content exceeds the 1 MB limit.";
            }

            Files.writeString(
                    path,
                    content,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );

            return "File created successfully: " + filePath;

        } catch (Exception e) {

            return "Failed to create file: " + e.getMessage();

        }
    }


    // --------------------------------------------------
    // 3. READ FILE
    // --------------------------------------------------

    @Tool(
            name = "read_file",
            description = """
            Read the complete contents of an existing HTML, CSS, or
            JavaScript file.

            Use this tool to inspect existing website code before
            modifying it.

            Always read the existing file when you need to understand
            its current implementation or make modifications.
            """
    )
    public String readFile(

            @ToolParam(description = """
                Relative file path to read.
                Example: portfolio/index.html
                """)
            String filePath

    ) {

        try {

            Path path = resolveSafePath(filePath);

            validateFileExtension(path);

            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "File does not exist: " + filePath;
            }

            if (Files.size(path) > MAX_FILE_SIZE) {
                return "File exceeds the 1 MB reading limit.";
            }

            String content = Files.readString(path);

            return """
                    File: %s

                    Content:
                    %s
                    """.formatted(filePath, content);

        } catch (Exception e) {

            return "Failed to read file: " + e.getMessage();

        }
    }


    // --------------------------------------------------
    // 4. WRITE / EDIT FILE
    // --------------------------------------------------

    @Tool(
            name = "write_file",
            description = """
            Write new content or completely replace the existing content
            of an HTML, CSS, or JavaScript file.

            Use this tool when generating website code or modifying
            an existing website file.

            IMPORTANT:
            Read the existing file first if you are modifying existing
            functionality. Preserve unrelated functionality.
            Provide the complete updated file content.
            """
    )
    public String writeFile(

            @ToolParam(description = """
                Relative path of the file to write.
                Example: portfolio/index.html
                """)
            String filePath,

            @ToolParam(description = """
                Complete updated file content.
                This replaces the existing file content.
                """)
            String content

    ) {

        try {

            Path path = resolveSafePath(filePath);

            validateFileExtension(path);

            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "File does not exist. Use create_file first.";
            }

            if (content.length() > MAX_FILE_SIZE) {
                return "File content exceeds the 1 MB limit.";
            }

            Files.writeString(
                    path,
                    content,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            return "File updated successfully: " + filePath;

        } catch (Exception e) {

            return "Failed to write file: " + e.getMessage();

        }
    }


    // --------------------------------------------------
    // 5. LIST FILES
    // --------------------------------------------------

    @Tool(
            name = "list_files",
            description = """
            List all files and subdirectories inside a specified
            workspace directory.

            Use this tool to understand the existing website structure
            before creating new files or modifying existing ones.

            Provide an empty path to list the root workspace.
            """
    )
    public String listFiles(

            @ToolParam(description = """
                Relative directory path to inspect.
                Example: portfolio or portfolio/css.
                Provide an empty string to list the workspace root.
                """)
            String directoryPath

    ) {

        try {

            Path path = directoryPath == null || directoryPath.isBlank()
                    ? rootPath
                    : resolveSafePath(directoryPath);

            if (!Files.exists(path) || !Files.isDirectory(path)) {
                return "Directory does not exist: " + directoryPath;
            }

            try (Stream<Path> files = Files.walk(path)) {

                String result = files
                        .filter(p -> !p.equals(path))
                        .filter(p -> !Files.isSymbolicLink(p))
                        .filter(p -> Files.isRegularFile(p)
                                || Files.isDirectory(p))
                        .map(p -> {
                            String type = Files.isDirectory(p)
                                    ? "[DIR] "
                                    : "[FILE] ";

                            return type + rootPath.relativize(p);
                        })
                        .collect(Collectors.joining("\n"));

                return result.isBlank()
                        ? "Directory is empty."
                        : result;

            }

        } catch (Exception e) {

            return "Failed to list files: " + e.getMessage();

        }
    }


    // --------------------------------------------------
    // SECURITY: SAFE PATH VALIDATION
    // --------------------------------------------------

    private Path resolveSafePath(String relativePath) throws IOException {

        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty.");
        }

        Path requested = Paths.get(relativePath);

        if (requested.isAbsolute()) {
            throw new SecurityException("Absolute paths are not allowed.");
        }

        Path resolved = rootPath.resolve(requested).normalize();

        if (!resolved.startsWith(rootPath) || resolved.equals(rootPath)) {
            throw new SecurityException(
                    "Access outside the workspace is not allowed."
            );
        }

        Path current = rootPath;

        for (Path part : rootPath.relativize(resolved)) {

            current = current.resolve(part);

            if (Files.isSymbolicLink(current)) {
                throw new SecurityException(
                        "Symbolic links are not allowed."
                );
            }
        }

        return resolved;
    }


    // --------------------------------------------------
    // FILE EXTENSION VALIDATION
    // --------------------------------------------------

    private void validateFileExtension(Path path) {

        String filename = path.getFileName()
                .toString()
                .toLowerCase();

        for (String extension : ALLOWED_EXTENSIONS) {

            if (filename.endsWith(extension)) {
                return;
            }
        }

        throw new SecurityException(
                "Only HTML, CSS, and JavaScript files are allowed."
        );
    }

}