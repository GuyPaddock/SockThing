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

import com.google.gson.Gson;

public class FileBackedCheckpointer
implements Checkpointer, CheckpointListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FileBackedCheckpointer.class);

    private static final String FILE_STORE_UNPROCESSED_DIRECTORY_PATH   = "filestore/unprocessed";
    private static final String FILE_STORE_PROCESSED_DIRECTORY_PATH     = "filestore/processed";

    private final Set<Checkpointable> registeredCheckpointables;
    private final Map<CheckpointItem, File> knownCheckpointItems;

    public FileBackedCheckpointer()
    {
        this.registeredCheckpointables  = Collections.synchronizedSet(new LinkedHashSet<Checkpointable>());
        this.knownCheckpointItems       = Collections.synchronizedMap(new HashMap<CheckpointItem, File>());
    }

    @Override
    public void setupCheckpointing(Checkpointable... checkpointables)
    {
        this.registeredCheckpointables.addAll(Arrays.asList(checkpointables));

        for (Checkpointable checkpointable : checkpointables)
        {
            checkpointable.registerCheckpointListener(this);
        }
    }

    @Override
    public void restoreCheckpointsFromDisk()
    {
        File unprocessedFileStore = new File(FILE_STORE_UNPROCESSED_DIRECTORY_PATH);

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
        File checkpointFile =
            this.getCheckpointItemFile(FILE_STORE_UNPROCESSED_DIRECTORY_PATH, checkpointable, checkpointItem);

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
    public void onCheckpointItemExpired(Checkpointable checkpointable, CheckpointItem checkpointItem)
    {
        File checkpointFile = this.knownCheckpointItems.get(checkpointItem);

        if ((checkpointFile != null) && checkpointFile.exists())
        {
            File processedCheckpointFile =
                this.getCheckpointItemFile(FILE_STORE_PROCESSED_DIRECTORY_PATH, checkpointable, checkpointItem);

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

        pathsToExplore.add(
            this.getCheckpointStorageDirectory(FILE_STORE_UNPROCESSED_DIRECTORY_PATH, checkpointable));

        do
        {
            File currentPath = pathsToExplore.pop();

            if (currentPath.isFile())
                results.add(currentPath);

            else
                pathsToExplore.addAll(Arrays.asList(currentPath.listFiles()));
        }
        while (!pathsToExplore.isEmpty());

        Collections.sort(results);

        return results;
    }

    protected File getCheckpointStorageDirectory(String basePath, Checkpointable checkpointable)
    {
        return new File(
            basePath                                + File.separator +
            checkpointable.getCheckpointableName()  + File.separator);
    }

    protected File getCheckpointItemFile(String basePath, Checkpointable checkpointable, CheckpointItem checkpoint)
    {
        return new File(
            basePath                                + File.separator +
            checkpointable.getCheckpointableName()  + File.separator +
            checkpoint.getCheckpointType()          + File.separator +
            checkpoint.getCheckpointId()            + ".json");
    }
}