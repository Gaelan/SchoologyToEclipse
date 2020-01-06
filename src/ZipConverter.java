import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class ZipConverter {
    final ArrayList<String> unknownTypeSubmissions = new ArrayList<>();

    /**
     * Get a list of projects
     */
    public ArrayList<String> getUnknownTypeSubmissions() {
        return unknownTypeSubmissions;
    }

    final ArrayList<String> multipleProjectsSubmissions = new ArrayList<>();

    public ArrayList<String> getMultipleProjectsSubmissions() {
        return multipleProjectsSubmissions;
    }

    Path tempFolder;
    String assignmentName;
    FileSystem outputZipFs;

    /**
     * Given a path to a zip file uploaded from schoology, create a zip file
     * suitable for import into Eclipse and return its path.
     */
    Path convertZip(Path inputZipPath) throws IOException {
        FileSystem inputZipFs = FileSystems.newFileSystem(inputZipPath, null);

        Path outputZipPath = inputZipPath.resolveSibling("ECLIPSE_" + inputZipPath.getFileName());
        Files.deleteIfExists(outputZipPath);
        outputZipFs = createOutputZip(outputZipPath);

        tempFolder = Files.createTempDirectory("SchoologyToEclipse");
        assignmentName = inputZipPath.getFileName().toString().replace(".zip", "");

        for (Path studentDir : Files.list(inputZipFs.getPath("/")).collect(Collectors.toList())) {
            String studentName = studentDir.getFileName().toString().split(" - ")[0];
            for (Path revisionDir : Files.list(studentDir).collect(Collectors.toList())) {
                handleProject(studentName, revisionDir);
            }
        }

        outputZipFs.close();
        return outputZipPath;
    }

    /**
     * Move a project into the new zip file, renaming it or converting it into
     * an Eclipse project if necessary.
     * @param studentName The student's name.
     * @param projectDirPath The path to the directory in the source zip
     *                       containing the student's submission.
     */
    void handleProject(String studentName, Path projectDirPath) throws IOException {
        String revisionName = projectDirPath.getFileName().toString().replace("/", "");
        String projectName = studentName + " " + revisionName + " " + assignmentName;
        Path submittedFilePath = getOnlyItemInFolder(projectDirPath);
        Path outputPath = outputZipFs.getPath("/" + projectName);

        if (!submittedFilePath.getFileName().toString().endsWith(".zip")) {
            if (submittedFilePath.getFileName().toString().endsWith(".java")) {
                // They submitted a raw java file. Wrap it in an Eclipse project.
                Files.createDirectory(outputPath);
                Path srcPath = outputPath.resolve("src");
                Files.createDirectory(srcPath);
                Files.copy(submittedFilePath, srcPath.resolve(submittedFilePath.getFileName().toString()));
                generateEclipseProject(outputPath, projectName);
            } else {
                unknownTypeSubmissions.add(studentName + " (" + revisionName + ")");
            }
            return;
        }

        // Java can't read out of a zip in a zip, so copy the project zip into
        // a temporary folder then read out of it.
        Path tempZipPath = tempFolder.resolve(submittedFilePath.getFileName().toString());
        Files.copy(submittedFilePath, tempZipPath);
        FileSystem projectZipFs = FileSystems.newFileSystem(tempZipPath, null);

        Path projectFolder = getOnlyItemInFolder(projectZipFs.getPath("/"));

        Files.createDirectory(outputPath);
        copyFolder(projectFolder, outputPath);

        // Find the .project file containing Eclipse metadata.
        List<Path> projectFiles = Files.walk(outputPath)
                .filter(path -> path.getFileName().toString().equals(".project"))
                .collect(Collectors.toList());

        if (projectFiles.isEmpty()) {
            // No eclipse project, let's create one.
            generateEclipseProject(outputPath, projectName);
        } else if (projectFiles.size() != 1) {
            // Multiple eclipse projects
            multipleProjectsSubmissions.add(studentName + " (" + revisionName + ")");
        } else {
            // There is an existing project. Change its name.
            Path projectFile = projectFiles.get(0);
            renameProject(projectName, projectFile);
        }

        // Delete the temporary zip to avoid issues with another zip with the
        // same name.
        projectZipFs.close();
        Files.delete(tempZipPath);
    }

    /**
     * Given a directory containing a Java project (i.e. source files in an
     * "src" subdirectory), add metadata files for a simple Eclipse project.
     * @param projectPath The path to the project.
     * @param projectName The name to use for the project.
     */
    void generateEclipseProject(Path projectPath, String projectName) throws IOException {
        String projectFile = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<projectDescription>\n" +
                "  <name>" + projectName + "</name>\n" +
                "  <comment></comment>\n" +
                "  <projects>\n" +
                "  </projects>\n" +
                "  <buildSpec>\n" +
                "    <buildCommand>\n" +
                "      <name>org.eclipse.jdt.core.javabuilder</name>\n" +
                "      <arguments>\n" +
                "      </arguments>\n" +
                "    </buildCommand>\n" +
                "  </buildSpec>\n" +
                "  <natures>\n" +
                "    <nature>org.eclipse.jdt.core.javanature</nature>\n" +
                "  </natures>\n" +
                "</projectDescription>";
        Files.write(projectPath.resolve(".project"), projectFile.getBytes());

        String classpathFile = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<classpath>\n" +
                "  <classpathentry kind=\"src\" path=\"src\"/>\n" +
                "  <classpathentry kind=\"con\" path=\"org.eclipse.jdt.launching.JRE_CONTAINER\" />\n" +
                "  <classpathentry kind=\"output\" path=\"bin\"/>\n" +
                "</classpath>";
        Files.write(projectPath.resolve(".classpath"), classpathFile.getBytes());
    }

    /**
     * Change the name in Eclipse's project metadata.
     * @param projectName The new name for the project.
     * @param projectFile The path to the project's .project file.
     */
    void renameProject(String projectName, Path projectFile) throws IOException {
        String projectFileText = new String(Files.readAllBytes(projectFile));
        projectFileText = projectFileText.replaceAll("<name>.*</name>", "<name>" + projectName + "</name>");
        Files.write(projectFile, projectFileText.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Create an empty zip file to use for output.
     * @param outputZipPath The path of the zip file.
     */
    FileSystem createOutputZip(Path outputZipPath) throws IOException {
        URI outputUri = outputZipPath.toUri();
        URI outputZipUri = URI.create("jar:" + outputUri.toString());
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        return FileSystems.newFileSystem(outputZipUri, env);
    }

    /**
     * If the passed folder contains exactly one item, return its path. If it
     * contains zero or multiple items, throw an exception.
     */
    Path getOnlyItemInFolder(Path folder) throws IOException {
        List<Path> files = Files.list(folder).collect(Collectors.toList());
        if (files.size() == 1) {
            return files.get(0);
        } else {
            throw new IllegalStateException("Expected " + folder + " to contain exactly one item, but it contains " + files.size());
        }
    }

    // Next two methods borrowed from https://stackoverflow.com/a/50418060/1629243

    public void copyFolder(Path src, Path dest) throws IOException {
        // This works around some bug involving trailing slashes with Java's
        // zip handling.
        Files.walk(src)
                .forEach(source -> copy(source, dest.resolve(src.resolveSibling(src.getFileName().toString().replace("/", "")).relativize(source))));
    }

    void copy(Path source, Path dest) {
        try {
            Files.copy(source, dest, REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}