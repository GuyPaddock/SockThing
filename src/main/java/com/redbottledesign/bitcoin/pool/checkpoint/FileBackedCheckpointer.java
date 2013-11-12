package com.redbottledesign.bitcoin.pool.checkpoint;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.Config;
import com.github.fireduck64.sockthing.StratumServer;
import com.google.gson.Gson;

public class FileBackedCheckpointer
implements Checkpointer, CheckpointListener
{
    private static final String CONFIG_PATH_FILE_STORE                  = "file_store_path";
    private static final String FILE_STORE_UNPROCESSED_DIRECTORY_PATH   = "unprocessed";
    private static final String FILE_STORE_PROCESSED_DIRECTORY_PATH     = "processed";
    private static final String FILENAME_SUFFIX_JSON = ".json";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileBackedCheckpointer.class);

    private final StratumServer server;
    private final Set<Checkpointable> registeredCheckpointables;
    private final Map<CheckpointItem, File> knownCheckpointItems;

    private String fileStorePath;

    public FileBackedCheckpointer(StratumServer server)
    {
        this.server = server;

        this.registeredCheckpointables  = Collections.synchronizedSet(new LinkedHashSet<Checkpointable>());
        this.knownCheckpointItems       = Collections.synchronizedMap(new HashMap<CheckpointItem, File>());
    }

    @Override
    public void setupCheckpointing(Checkpointable... checkpointables)
    {
        this.loadConfig();

        this.registeredCheckpointables.addAll(Arrays.asList(checkpointables));

        for (Checkpointable checkpointable : checkpointables)
        {
            checkpointable.registerCheckpointListener(this);
        }
    }

    @Override
    public void restoreCheckpointsFromDisk()
    {
        File unprocessedFileStore = this.getFileStoreDirectory(FILE_STORE_UNPROCESSED_DIRECTORY_PATH);

        if (unprocessedFileStore.exists())
        {
            for (Checkpointable checkpointable : this.registeredCheckpointables)
            {
                Type                    itemType        = checkpointable.getCheckpointItemType();
                List<File>              checkpointFiles = this.getSavedCheckpoint(checkpointable);
                List<CheckpointItem>    restoredItems   = new LinkedList<>();

                for (File checkpointFile : checkpointFiles)
                {
                    try
                    {
                        CheckpointItem readCheckpointItem;

                        if (LOGGER.isInfoEnabled())
                            LOGGER.info(String.format("Reading in saved checkpoint '%s'.", checkpointFile.getPath()));

                        readCheckpointItem = this.readInCheckpointItem(itemType, checkpointFile);

                        restoredItems.add(readCheckpointItem);
                        this.knownCheckpointItems.put(readCheckpointItem, checkpointFile);
                    }

                    catch (IOException ex)
                    {
                        if (LOGGER.isErrorEnabled())
                        {
                            LOGGER.error(
                                String.format(
                                    "Failed to read-in checkpoint from '%s': %s\n%s",
                                    checkpointFile.toString(),
                                    ex.getMessage(),
                                    ExceptionUtils.getStackTrace(ex)));
                        }
                    }
                }

                if (!restoredItems.isEmpty())
                    checkpointable.restoreFromCheckpoint(restoredItems);
            }
        }
    }

    @Override
    public void onCheckpointItemCreated(Checkpointable checkpointable, CheckpointItem checkpointItem)
    {
        File checkpointFile = this.generateCheckpointItemFile(FileStoreType.UNPROCESSED, checkpointable, checkpointItem);

        try
        {
            if (LOGGER.isInfoEnabled())
                LOGGER.info(String.format("Writing out pre-save checkpoint item to '%s'.", checkpointFile.getPath()));

            this.writeOutCheckpointItem(checkpointItem, checkpointFile);
            this.knownCheckpointItems.put(checkpointItem, checkpointFile);
        }

        catch (IOException ex)
        {
            String error =
                String.format(
                    "Failed to write out checkpoint item to '%s': %s\n\n%s\n%s",
                    checkpointFile.toString(),
                    checkpointItem,
                    ex.getMessage(),
                    ExceptionUtils.getStackTrace(ex));

            if (LOGGER.isErrorEnabled())
                LOGGER.error(error);

            // This is pretty serious.
            System.err.printf(error);
            System.exit(-1);
        }
    }

    @Override
    public void onCheckpointItemUpdated(Checkpointable checkpointable, CheckpointItem checkpointItem)
    {
        File checkpointFile = this.getCheckpointFile(checkpointItem);

        if (checkpointFile != null)
        {
            try
            {
                if (LOGGER.isInfoEnabled())
                    LOGGER.info(String.format("Updating checkpoint item '%s'.", checkpointFile.getPath()));

                this.writeOutCheckpointItem(checkpointItem, checkpointFile);
            }

            catch (IOException ex)
            {
                String error =
                    String.format(
                        "Failed to update checkpoint item to '%s': %s\n\n%s\n%s",
                        checkpointFile.toString(),
                        checkpointItem,
                        ex.getMessage(),
                        ExceptionUtils.getStackTrace(ex));

                if (LOGGER.isErrorEnabled())
                    LOGGER.error(error);

                // This is pretty serious.
                System.err.printf(error);
                System.exit(-1);
            }
        }
    }

    @Override
    public void onCheckpointItemExpired(Checkpointable checkpointable, CheckpointItem checkpointItem)
    {
        File checkpointFile = this.getCheckpointFile(checkpointItem);

        if (checkpointFile != null)
        {
            File processedCheckpointFile =
                this.generateCheckpointItemFile(FileStoreType.PROCESSED, checkpointable, checkpointItem);

            // Remove the original (now processed) file
            checkpointFile.delete();

            try
            {
                if (LOGGER.isInfoEnabled())
                {
                    LOGGER.info(
                        String.format(
                            "Updating and moving processed checkpoint item to '%s'.",
                            processedCheckpointFile.getPath()));
                }

                // Write-out the final copy of the checkpoint
                this.writeOutCheckpointItem(checkpointItem, processedCheckpointFile);
            }

            catch (IOException ex)
            {
                if (LOGGER.isErrorEnabled())
                {
                    LOGGER.error(
                        String.format(
                            "Failed to write out updated checkpoint item to '%s': %s\n%s",
                            checkpointFile.getPath(),
                            ex.getMessage(),
                            ExceptionUtils.getStackTrace(ex)));
                }
            }
        }

        // Stop tracking the processed item
        this.knownCheckpointItems.remove(checkpointItem);
    }

    protected void loadConfig()
    {
        Config config = this.server.getConfig();

        config.require(CONFIG_PATH_FILE_STORE);

        this.fileStorePath = config.get(CONFIG_PATH_FILE_STORE);
    }

    protected void writeOutCheckpointItem(CheckpointItem checkpointItem, File destinationFile)
    throws IOException
    {
        Gson    gson    = CheckpointGsonBuilder.getInstance().getGson();
        String  json    = gson.toJson(checkpointItem);

        // Create directory structure
        destinationFile.getParentFile().mkdirs();

        try (FileWriter fileWriter = new FileWriter(destinationFile))
        {
            fileWriter.write(json);
        }
    }

    protected CheckpointItem readInCheckpointItem(Type itemType, File sourceFile)
    throws IOException
    {
        CheckpointItem  result;
        Gson            gson    = CheckpointGsonBuilder.getInstance().getGson();

        try (FileReader fileReader = new FileReader(sourceFile))
        {
            result = gson.fromJson(fileReader, itemType);
        }

        return result;
    }

    protected List<File> getSavedCheckpoint(Checkpointable checkpointable)
    {
        List<File>  results         = new LinkedList<>();
        Deque<File> pathsToExplore  = new LinkedList<>();

        pathsToExplore.add(this.getCheckpointStorageDirectory(FileStoreType.UNPROCESSED, checkpointable));

        do
        {
            File currentPath = pathsToExplore.pop();

            if (currentPath.isFile())
            {
                results.add(currentPath);
            }

            else
            {
                File[] filesInPath = currentPath.listFiles();

                if ((filesInPath != null) && (filesInPath.length > 0))
                    pathsToExplore.addAll(Arrays.asList(filesInPath));
            }
        }
        while (!pathsToExplore.isEmpty());

        Collections.sort(results);

        return results;
    }

    protected File getFileStoreDirectory(String subPath)
    {
        return new File(this.fileStorePath + "/" + subPath);
    }

    protected File getCheckpointStorageDirectory(FileStoreType storeType, Checkpointable checkpointable)
    {
        return new File(
            this.fileStorePath                      + File.separator +
            storeType.getRelativePath()             + File.separator +
            checkpointable.getCheckpointableName()  + File.separator);
    }

    protected File getCheckpointFile(CheckpointItem checkpointItem)
    {
        File checkpointFile = this.knownCheckpointItems.get(checkpointItem);

        if (checkpointFile == null)
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Notification received about checkpoint item '%s', but there is no such checkpoint item " +
                        "being tracked by this checkpointer.",
                        checkpointItem));
            }
        }

        else if (!checkpointFile.exists())
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Notification received about checkpoint item '%s', but checkpoint file (%s) is missing!",
                        checkpointItem,
                        checkpointFile.getAbsolutePath()));
            }
        }

        return checkpointFile;
    }

    protected File generateCheckpointItemFile(FileStoreType storeType, Checkpointable checkpointable,
                                              CheckpointItem checkpoint)
    {
        String  checkpointStoragePath = this.getCheckpointStorageDirectory(storeType, checkpointable).getPath(),
                candidateFilename;
        int     candidateIndex        = 0;
        File    candidateFile;

        candidateFilename =
            checkpointStoragePath           + File.separator +
            checkpoint.getCheckpointType()  + File.separator +
            checkpoint.getCheckpointId();

        candidateFile = new File(candidateFilename + FILENAME_SUFFIX_JSON);

        while (candidateFile.exists())
        {
            if (LOGGER.isErrorEnabled())
            {
                LOGGER.error(
                    String.format(
                        "Checkpoint file '%s' already exists?! All checkpoint filenames should be unique. " +
                        "Attempting to generate a unique filename.",
                        candidateFile.getAbsolutePath()));
            }

            candidateFile =
                new File(String.format("%s-%d%s", candidateFilename, ++candidateIndex, FILENAME_SUFFIX_JSON));
        }

        return candidateFile;
    }

    protected enum FileStoreType
    {
        PROCESSED(  FILE_STORE_PROCESSED_DIRECTORY_PATH),
        UNPROCESSED(FILE_STORE_UNPROCESSED_DIRECTORY_PATH);

        private final String relativePath;

        private FileStoreType(String relativePath)
        {
            this.relativePath = relativePath;
        }

        public String getRelativePath()
        {
            return this.relativePath;
        }
    }
}