package com.redbottledesign.bitcoin.pool;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.fireduck64.sockthing.PoolUser;
import com.github.fireduck64.sockthing.PoolUserExtension;

/**
 * A utility class for adjusting a pool user's target difficulty based on the
 * difficulty of the shares they've submitted in the last fifteen minutes.
 *
 * @author Guy Paddock (guy.paddock@redbottledesign.com)
 */
public class VardiffCalculator
{
    /**
     * How often, in milliseconds, that worker difficulties should be re-computed.
     */
    private static final long COMPUTATION_INTERVAL = TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);

    /**
     * Minimum number of shares needed to re-compute a worker's difficulty.
     */
    private static final int MIN_SHARES_FOR_COMPUTATION = 5;

    /**
     * The target number of shares per second desired for each worker
     * (1 share every 5 seconds).
     */
    private static final double TARGET_SHARE_RATE_MS = (1d / TimeUnit.MILLISECONDS.convert(5, TimeUnit.SECONDS));

    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(VardiffCalculator.class);

    /**
     * The current instance.
     */
    private static VardiffCalculator INSTANCE;

    /**
     * Static constructor.
     */
    static
    {
        INSTANCE = new VardiffCalculator();
    }

    /**
     * Gets the current instance;
     *
     * @return  The current instance.
     */
    public static VardiffCalculator getInstance()
    {
        return INSTANCE;
    }

    /**
     * Protected constructor to enforce singleton pattern.
     */
    protected VardiffCalculator()
    {
    }

    /**
     * <p>Computes a difficulty adjustment for the specified pool user, if one
     * is necessary.</p>
     *
     * <p>The work share difficulty that is provided is factored into the
     * computation in one of two ways:</p>
     * <ol>
     *  <li>If it is time to adjust the user's difficulty, the difficulty of
     *  the provided share is factored into the user's new difficulty
     *  immediately.</li>
     *
     *  <li>If it is not yet time to adjust the user's difficulty, the
     *  difficulty of the provided share is added to the list of difficulties
     *  that are taken into account the next time the user's difficulty is
     *  adjusted. This list is then cleared on the next adjustment.</li>
     * </ol>
     *
     * <p>A user's difficulty is set to half their average difficulty since
     * the last computation.</p>
     *
     * @param   user
     *          The user for which difficulty is being computed.
     *
     * @param   shareDifficulty
     *          The difficulty of the work share that was last submitted by the
     *          user.
     *
     * @return  {@code true} if the user's difficulty was actually modified;
     *          or, {@code false} otherwise.
     */
    public boolean computeDifficultyAdjustment(PoolUser user, double shareDifficulty)
    {
        boolean difficultyChanged = false;

        /* Synchronized to prevent multiple updates to the same user if
         * multiple shares come in at the same time.
         */
        synchronized (user)
        {
            VardiffExtension varDiffExt = user.getExtension(VardiffExtension.class);

            if (varDiffExt == null)
            {
                varDiffExt = new VardiffExtension();

                user.putExtension(varDiffExt);
            }

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                    String.format(
                        "Factoring-in share of difficulty %f to vardiff calculation for worker %s.%s",
                        shareDifficulty,
                        user.getName(),
                        user.getWorkerName()));
            }

            varDiffExt.addShareDifficulty(shareDifficulty);

            if (varDiffExt.isTimeForUpdate())
            {
                double  difficultyMedian    = varDiffExt.getMedianDifficulty(),
                        shareRate           = varDiffExt.getShareRate(),
                        difficultyScalar    = shareRate / TARGET_SHARE_RATE_MS;
                int     targetDifficulty;

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(
                        String.format(
                            "Computing new minimum share difficulty for worker %s.%s. Since '%s', the median " +
                            "difficulty for the worker was %2f and the share rate was %.4f shares per second (target " +
                            "is %.4f shares per second).",
                            user.getName(),
                            user.getWorkerName(),
                            varDiffExt.getLastUpdate(),
                            difficultyMedian,
                            shareRate * TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS),
                            TARGET_SHARE_RATE_MS * TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)));
                }

                /* Adjust user's difficulty as a factor of their median, scaled
                 * by how their current speed relates to the target speed.
                 */
                targetDifficulty = (int)(difficultyMedian * difficultyScalar);

                // Make sure difficulty is never zero
                if (targetDifficulty < 1)
                    targetDifficulty = 1;

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(
                        String.format(
                            "Setting target difficulty for worker %s.%s to %d (scalar: %f).",
                            user.getName(),
                            user.getWorkerName(),
                            targetDifficulty,
                            difficultyScalar));
                }

                difficultyChanged = (user.getDifficulty() != targetDifficulty);

                user.setDifficulty(targetDifficulty);

                // Clear most recent difficulty list
                varDiffExt.clearShareDifficulties();
                varDiffExt.markUpdated();
            }
        }

        if (difficultyChanged && LOGGER.isInfoEnabled())
        {
            LOGGER.info(
                String.format(
                    "Changed minimum share difficulty for worker %s.%s to %d.",
                    user.getName(),
                    user.getWorkerName(),
                    user.getDifficulty()));
        }

        else if (!difficultyChanged && LOGGER.isDebugEnabled())
        {
            LOGGER.debug(
                String.format(
                    "Minimum share difficulty for worker %s.%s was not affected.",
                    user.getName(),
                    user.getWorkerName(),
                    user.getDifficulty()));
        }

        return difficultyChanged;
    }

    protected static class VardiffExtension
    implements PoolUserExtension
    {
        private Date lastUpdate;
        private final List<Double> shareDifficulties;

        public VardiffExtension()
        {
            this.lastUpdate         = new Date();
            this.shareDifficulties  = new LinkedList<>();
        }

        public Date getLastUpdate()
        {
            return this.lastUpdate;
        }

        public void setLastUpdate(Date lastUpdate)
        {
            this.lastUpdate = lastUpdate;
        }

        public void markUpdated()
        {
            this.setLastUpdate(new Date());
        }

        /**
         * Gets the number of milliseconds that have elapsed since the last
         * time this worker's difficulty was computed.
         *
         * @return  The number of milliseconds since the last update.
         */
        public long getDurationSinceLastUpdate()
        {
            return (new Date().getTime() - this.lastUpdate.getTime());
        }

        public boolean isTimeForUpdate()
        {
            return ((this.getDurationSinceLastUpdate() >= COMPUTATION_INTERVAL) &&
                    (this.shareDifficulties.size() >= MIN_SHARES_FOR_COMPUTATION));
        }

        public void clearShareDifficulties()
        {
            this.shareDifficulties.clear();
        }

        public void addShareDifficulty(double shareDifficulty)
        {
            this.shareDifficulties.add(shareDifficulty);
        }

        public List<Double> getShareDifficulties()
        {
            return Collections.unmodifiableList(this.shareDifficulties);
        }

        public double getMedianDifficulty()
        {
            return this.calculateMedian(this.shareDifficulties);
        }

        public double getShareRate()
        {
            return ((double)this.shareDifficulties.size()) / ((double)this.getDurationSinceLastUpdate());
        }

        protected double calculateMedian(List<Double> difficulties)
        {
            Double[]    difficultyArray = difficulties.toArray(new Double[difficulties.size()]);
            double      result;
            int         middleIndex;

            Arrays.sort(difficultyArray);

            middleIndex = (difficultyArray.length / 2);

            if ((difficultyArray.length % 2) == 1)
                result = difficultyArray[middleIndex];

            else
                result = (difficultyArray[middleIndex - 1] + difficultyArray[middleIndex]) / 2d;

            return result;
        }
    }

    public static void main(String[] args)
    {
        VardiffExtension extension = new VardiffExtension();


        for (int i = 1; i < 100; ++i)
        {
            System.out.printf("Adding %d...", i);
            extension.addShareDifficulty(i);

            System.out.println("Median is now: " + extension.getMedianDifficulty());
        }
    }
}