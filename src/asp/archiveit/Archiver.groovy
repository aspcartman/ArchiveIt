package asp.archiveit

import groovy.transform.CompileStatic

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * MIPT
 * Autor: aspcartman
 * Date: 19/01/15
 */

@CompileStatic
class StatStruct {
    Path path
    List<Path> files = new LinkedList<>()
    long foldersCount
    long size
}

@CompileStatic
abstract class ArchiverDelegate {
    abstract void willArchive()

    abstract void willScan()

    abstract void willScanFile(Path path)

    abstract void didScanFile(Path path)

    abstract void didScan(StatStruct stat)

    abstract void willCopy()

    abstract void willCopyFile(Path from, Path to)

    abstract void didCopyFile(Path from, Path to)

    abstract void didCopy()

    abstract void didTrash()

    abstract void didLink()

    abstract void didArchive()

    abstract void error(String wtf)
}

@CompileStatic
class Archiver {
    ArchiverDelegate delegate

    Path archivePath
    Path targetPath

    StatStruct targetScan

    void archive() {
        delegate?.willArchive()

        scan()
        copy()
        trash()
        link()

        delegate?.didArchive()
    }


    protected scan() {
        delegate?.willScan()

        StatStruct scan = new StatStruct(path: targetPath)

        if (Files.isDirectory(targetPath))
            Files.walkFileTree(targetPath, new FileVisitor<Path>() {
                @Override
                FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    scan.foldersCount++
                    FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    delegate?.willScanFile(file)

                    scanFile(scan, file)

                    delegate?.didScanFile(file)
                    FileVisitResult.CONTINUE
                }

                @Override
                FileVisitResult visitFileFailed(Path file, IOException exc) {
                    FileVisitResult.TERMINATE
                }

                @Override
                FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    FileVisitResult.CONTINUE
                }
            })
        else
            scanFile(scan, targetPath)

        targetScan = scan

        delegate?.didScan(scan)
    }

    private void scanFile(StatStruct scan, Path file) {
        scan.size += file.size()
        scan.files.add(file)
    }

    protected copy() {
        delegate?.willCopy()

        for (Path file in targetScan.files) {
            def dest = pathInArchive(file)
            delegate?.willCopyFile(file, dest)
            Files.createDirectories(dest.parent)
            Files.copy(file, dest, REPLACE_EXISTING, COPY_ATTRIBUTES)
            delegate?.didCopyFile(file, dest,)
        }

        delegate?.didCopy()
    }


    protected void trash() {
        Files.move(targetPath, Paths.get(System.getProperty("user.home"), ".Trash/", targetPath.fileName.toString()), REPLACE_EXISTING)
        delegate?.didTrash()
    }

    protected void link() {
        def archDir = Paths.get(targetPath.parent.toString(), "!Archive")
        if (Files.notExists(archDir))
            Files.createDirectory(archDir)
        archDir = Paths.get(archDir.toString(), targetPath.fileName.toString())
        Files.createSymbolicLink(archDir, pathInArchive(targetPath))
        delegate?.didLink()
    }

    private Path pathInArchive(Path file) {
        Paths.get(archivePath.toAbsolutePath().toString(), file.toAbsolutePath().toString())
    }
}


