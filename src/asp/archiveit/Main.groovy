/**
 * MIPT
 * Autor: aspcartman
 * Date: 18/01/15
 */

package asp.archiveit

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Main {
    static void main(String[] args) {
        def cli = new CliBuilder(usage: 'archiveit -[h] path')
        cli.with {
            h longOpt: 'help', 'Show this help'
            i longOpt: 'input', args: 1,
                    argName: 'path', 'Path to archive'
            o longOpt: 'output', args: 1,
                    argName: 'path', 'Archive path'
        }

        def options = cli.parse(args)
        if (!options)
            return

        if (options.h)
            cli.usage()

        def inputPath = Paths.get(options.i as String)
        def archivePath = Paths.get(options.o as String)
        assert Files.exists(inputPath); assert Files.exists(archivePath);

        Archiver a = new Archiver(targetPath: inputPath, archivePath: archivePath, delegate: new ArchiverDelegate() {
            @Override
            void willArchive() {
                println("Going to archive")
                println("<--" + inputPath)
                println("-->" + archivePath)
            }

            @Override
            void didArchive() {
                println("Done, my lord.")
            }

            @Override
            void willScan() {
                println('Scanning of folder structure')
            }

            @Override
            void willScanFile(Path path) {
                println(path)
            }

            @Override
            void didScanFile(Path path) {
                deletePrintedLine(1)
            }

            @Override
            void didScan(StatStruct stat) {
                println('To do: ' + [folders:stat.foldersCount, files:stat.files.size(), bytes:stat.size] )
            }

            @Override
            void willCopy() {
                println("Copying...")
            }

            @Override
            void willCopyFile(Path from, Path to) {

                println("<--" + from + '\n' + "-->" + to)
            }

            @Override
            void didCopyFile(Path from, Path to) {
                deletePrintedLine(2)
            }

            @Override
            void didCopy() {
                println("Copied.")
            }

            @Override
            void didTrash() {
                println("Trashed.")
            }

            @Override
            void didLink() {
                println("Linked.")
            }

            @Override
            void error(String wtf) {
                println("Error o_O")
            }
        })
        a.archive()
    }

    static void deletePrintedLine(int count)
    {
        count.times {
            print(String.format("\033[%dA", 1)); // Move up
            print("\033[2K");
        }
    }

}
