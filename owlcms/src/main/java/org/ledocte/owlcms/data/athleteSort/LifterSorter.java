/*
 * Copyright 2009-2012, Jean-François Lamy
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 */
package org.ledocte.owlcms.data.athleteSort;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.persistence.Entity;

import org.ledocte.owlcms.data.athlete.Athlete;
import org.ledocte.owlcms.data.category.Category;
import org.ledocte.owlcms.data.competition.Competition;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;

/**
 * @since
 * @author jflamy
 */
@Entity
public class LifterSorter implements Serializable {

    private static final long serialVersionUID = -3507146241019771820L;
    private static final Logger logger = (Logger) LoggerFactory.getLogger(LifterSorter.class);

    public enum Ranking {
        SNATCH, CLEANJERK, TOTAL, COMBINED, SINCLAIR, ROBI, CUSTOM
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @see #liftingOrder(List)
     * @return athletes, ordered according to their lifting order
     */
    static public List<Athlete> liftingOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);

        liftingOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to official rules.
     * <p>
     * <li>Lowest weight goes first</li>
     * <li>At same weight, lower attempt goes first</li>
     * <li>At same weight and same attempt, whoever lifted first goes first</li>
     * <li>At first attempt of each lift, lowest lot number goes first if same weight is requested</li>
     * </p>
     */
    static public void liftingOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new LiftOrderComparator());
        int liftOrder = 1;
        for (Athlete curLifter : toBeSorted) {
            curLifter.setLiftOrderRank(liftOrder++);
        }
    }

    /**
     * Sort athletes according to official rules (in place) <tableToolbar> <li>by category</li> <li>by lot number</li> </tableToolbar>
     */
    static public void displayOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new DisplayOrderComparator());
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @see #liftingOrder(List)
     * @return athletes, ordered according to their standard order
     */
    static public List<Athlete> displayOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
        displayOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to official rules (in place) for the technical meeting <tableToolbar> <li>by registration category</li> <li>by
     * lot number</li> </tableToolbar>
     */
    static public void registrationOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new RegistrationOrderComparator());
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @see #liftingOrder(List)
     * @return athletes, ordered according to their standard order for the technical meeting
     */
    static public List<Athlete> registrationOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
        registrationOrder(sorted);
        return sorted;
    }

    // /**
    // * Sort athletes according to official rules (in place) for the technical
    // * meeting <tableToolbar> <li>by registration category</li> <li>by lot
    // * number</li> </tableToolbar>
    // */
    // static public void weighInOrder(List<Athlete> toBeSorted) {
    // Collections.sort(toBeSorted, new WeighInOrderComparator());
    // }
    //
    // /**
    // * Sort athletes according to official rules, creating a new list.
    // *
    // * @see #liftingOrder(List)
    // * @return athletes, ordered according to their standard order for the
    // * technical meeting
    // */
    // static public List<Athlete> weighInOrderCopy(List<Athlete> toBeSorted) {
    // List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
    // weighInOrder(sorted);
    // return sorted;
    // }

    /**
     * Sort athletes according to official rules (in place) for the start number <tableToolbar> <li>by registration category</li> <li>by lot
     * number</li> </tableToolbar>
     */
    static public void startNumberOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new StartNumberOrderComparator());
    }

    /**
     * Sort athletes according to official rules, creating a new list.
     *
     * @see #liftingOrder(List)
     * @return athletes, ordered according to their start number
     */
    static public List<Athlete> startNumberOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
        startNumberOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to winning order, creating a new list.
     *
     * @see #liftingOrder(List)
     * @return athletes, ordered according to their category and totalRank order
     */
    static public List<Athlete> resultsOrderCopy(List<Athlete> toBeSorted, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
        resultsOrder(sorted, rankingType);
        return sorted;
    }

    /**
     * Sort athletes according to winning order.
     */
    static public void resultsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new WinningOrderComparator(rankingType));
        int liftOrder = 1;
        for (Athlete curLifter : toBeSorted) {
            curLifter.setResultOrderRank(liftOrder++, rankingType);
        }
    }

    /**
     * @param athletes
     * @param rankingType
     */
    @SuppressWarnings("unused")
    private void teamPointsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new TeamPointsOrderComparator(rankingType));
    }

    /**
     * @param athletes
     * @param rankingType
     */
    @SuppressWarnings("unused")
    private void combinedPointsOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new CombinedPointsOrderComparator(rankingType));
    }

    /**
     * Assign lot numbers at random.
     *
     * @param toBeSorted
     */
    static public List<Athlete> drawLots(List<Athlete> toBeShuffled) {
        List<Athlete> shuffled = new ArrayList<Athlete>(toBeShuffled);
        Collections.shuffle(shuffled, new Random());
        assignLotNumbers(shuffled);
        return shuffled;
    }

    /**
     * Assign lot numbers, sequentially. Normally called by {@link #drawLots(List)}.
     *
     * @param shuffledList
     */
    static public void assignLotNumbers(List<Athlete> shuffledList) {
        int lotNumber = 1;
        for (Athlete curLifter : shuffledList) {
            curLifter.setLotNumber(lotNumber++);
            curLifter.setStartNumber(0);
        }
    }

    /**
     * Sets the current Athlete as such (setCurrentLifter(true)), the others to false
     *
     * @param athletes
     *            Assumed to be already sorted in lifting order.
     */
    static public Athlete markCurrentLifter(List<Athlete> lifters) {
        if (!lifters.isEmpty()) {
            final Athlete firstLifter = lifters.get(0);
            firstLifter.setAsCurrentLifter(firstLifter.getAttemptsDone() < 6);
            for (Athlete Athlete : lifters) {
                if (Athlete != firstLifter) {
                    Athlete.setAsCurrentLifter(false);
                }
                Athlete.resetForcedAsCurrent();
            }
            return firstLifter;
        } else {
            return null;
        }
    }

    /**
     * Compute the number of lifts already done. During snatch, exclude
     *
     * @param athletes
     *            Assumed to be already sorted in lifting order.
     */
    static public int countLiftsDone(List<Athlete> lifters) {
        if (!lifters.isEmpty()) {
            int totalSnatch = 0;
            int totalCJ = 0;
            boolean cJHasStarted = false;
            for (Athlete Athlete : lifters) {
                totalSnatch += Athlete.getSnatchAttemptsDone();
                totalCJ += Athlete.getCleanJerkAttemptsDone();
                if (Athlete.getCleanJerkTotal() > 0) {
                    cJHasStarted = true;
                }
            }
            if (cJHasStarted || totalSnatch >= lifters.size() * 3) {
                return totalCJ;
            } else {
                return totalSnatch;
            }
        } else {
            return 0;
        }
    }

    /**
     * Sort athletes according to who lifted last, creating a new list.
     *
     * @see #liftTimeOrder(List)
     * @return athletes, ordered according to their lifting order
     * @param toBeSorted
     */
    static public List<Athlete> LiftTimeOrderCopy(List<Athlete> toBeSorted) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
        liftTimeOrder(sorted);
        return sorted;
    }

    /**
     * Sort athletes according to who lifted last.
     */
    static public void liftTimeOrder(List<Athlete> toBeSorted) {
        Collections.sort(toBeSorted, new LiftTimeStampComparator());
    }

    /**
     * Sort athletes by team, gender and totalRank so team totals can be computed
     *
     * @param athletes
     * @param rankingType
     *            what type of lift or total is being ranked
     * @return
     */
    public static List<Athlete> teamRankingOrderCopy(List<Athlete> toBeSorted, Ranking rankingType) {
        List<Athlete> sorted = new ArrayList<Athlete>(toBeSorted);
        teamRankingOrder(sorted, rankingType);
        return sorted;
    }

    /**
     *
     * Sort athletes by team, gender and totalRank so team totals can be assigned
     *
     * @param athletes
     * @return
     */
    static public void teamRankingOrder(List<Athlete> toBeSorted, Ranking rankingType) {
        Collections.sort(toBeSorted, new TeamRankingComparator(rankingType));
    }

    /**
     * Check that Athlete is one of the howMany previous athletes. The list of athletes is assumed to have been sorted with
     * {@link #liftTimeOrderCopy}
     *
     * @see #liftingOrder(List)
     * @return true if Athlete is found and meets criterion.
     * @param toBeSorted
     */
    static public boolean isRecentLifter(Athlete Athlete, List<Athlete> sortedLifters, int howMany) {
        int rank = sortedLifters.indexOf(Athlete);
        if (rank >= 0 && rank <= howMany - 1)
            return true;
        return false;
    }

    /**
     * Assign start numbers to athletes
     *
     * @param athletes
     */
    public static void assignStartNumbers(List<Athlete> sortedList) {
        int rank = 1;
        for (Athlete curLifter : sortedList) {
            Double bodyWeight = curLifter.getBodyWeight();
            if (bodyWeight != null && bodyWeight > 0.0D) {
                curLifter.setStartNumber(rank);
                rank++;
            } else {
                curLifter.setStartNumber(0);
            }

        }
    }

    // /**
    // * Assign medals, sequentially.
    // *
    // * @param sortedList
    // */
    // static public void assignMedals(List<Athlete> sortedList) {
    // Category prevCategory = null;
    // Integer prevAgeGroup = null;
    // Integer curAgeGroup = null;
    //
    // int rank = 1;
    // for (Athlete curLifter : sortedList) {
    // Category curCategory = null;
    // if (WebApplicationConfiguration.useRegistrationCategory) {
    // curCategory = curLifter.getRegistrationCategory();
    // } else {
    // curCategory = curLifter.getCategory();
    // }
    // if (Competition.isMasters()) {
    // curAgeGroup = curLifter.getAgeGroup();
    // }
    //
    // if (!equals(curCategory, prevCategory) || !equals(curAgeGroup, prevAgeGroup)) {
    // // category boundary has been crossed
    // rank = 1;
    // }
    //
    // if (curLifter.isInvited()) {
    // logger.trace("Athlete {}  totalRank={} total={}",
    //                		new Object[] { curLifter, -1, curLifter.getTotal() }); //$NON-NLS-1$
    // curLifter.setRank(-1);
    // } else if (rank <= 3 && curLifter.getTotal() > 0) {
    // logger.trace("Athlete {}  totalRank={} total={}",
    //                		new Object[] { curLifter, rank, curLifter.getTotal() }); //$NON-NLS-1$
    // curLifter.setRank(rank);
    // rank++;
    // } else {
    // logger.trace("Athlete {}  totalRank={} total={}",
    //                		new Object[] { curLifter, 0, curLifter.getTotal() }); //$NON-NLS-1$
    // curLifter.setRank(0);
    // rank++;
    // }
    // prevCategory = curCategory;
    // prevAgeGroup = curAgeGroup;
    // }
    // }

    /**
     * Assign ranks, sequentially.
     *
     * @param sortedList
     */
    public static void assignCategoryRanks(List<Athlete> sortedList, Ranking rankingType) {
        Category prevCategory = null;
        Integer prevAgeGroup = null;
        Integer curAgeGroup = null;

        int rank = 1;
        for (Athlete curLifter : sortedList) {
            Category curCategory = null;
            if (Competition.getCurrent().isUseRegistrationCategory() || rankingType == Ranking.CUSTOM) {
                curCategory = curLifter.getRegistrationCategory();
                if (curCategory == null && rankingType == Ranking.CUSTOM) {
                    curCategory = curLifter.getCategory();
                }
                logger.trace("Athlete {}, category {}, regcategory {}",
                        new Object[] { curLifter, curLifter.getCategory(), curLifter.getRegistrationCategory() });
            } else {
                curCategory = curLifter.getCategory();
            }
            if (Competition.getCurrent().isMasters()) {
                curAgeGroup = curLifter.getAgeGroup();
                if (!equals(curCategory, prevCategory) || !equals(curAgeGroup, prevAgeGroup)) {
                    // category boundary has been crossed
                    rank = 1;
                }
            } else {
                // not masters, only consider category boundary
                if (!equals(curCategory, prevCategory)) {
                    // category boundary has been crossed
                    logger.trace("category boundary crossed {}", curCategory);
                    rank = 1;
                }
            }

            if (curLifter.isInvited() || !curLifter.getTeamMember()) {
                logger.trace("not counted {}  {}Rank={} total={} {}",
                        new Object[] { curLifter, rankingType, -1, curLifter.getTotal(), curLifter.isInvited() }); //$NON-NLS-1$
                setRank(curLifter, -1, rankingType);
                setPoints(curLifter, 0, rankingType);
            } else {
                // if (curLifter.getTeamMember()) {
                // setTeamRank(curLifter, 0, rankingType);
                // }
                final double rankingTotal = getRankingTotal(curLifter, rankingType);
                if (rankingTotal > 0) {
                    setRank(curLifter, rank, rankingType);
                    logger.trace("Athlete {}  {}rank={} total={}",
                            new Object[] { curLifter, rankingType, getRank(curLifter, rankingType), rankingTotal }); //$NON-NLS-1$
                    rank++;
                } else {
                    logger.trace("Athlete {}  {}rank={} total={}",
                            new Object[] { curLifter, rankingType, 0, rankingTotal }); //$NON-NLS-1$
                    setRank(curLifter, 0, rankingType);
                    rank++;
                }
                final float points = computePoints(curLifter, rankingType);
                setPoints(curLifter, points, rankingType);

            }
            prevCategory = curCategory;
            prevAgeGroup = curAgeGroup;
        }
    }

    /**
     * Assign ranks, sequentially.
     *
     * @param sortedList
     */
    public static void assignSinclairRanksAndPoints(List<Athlete> sortedList, Ranking rankingType) {
        String prevGender = null;
        // String prevAgeGroup = null;
        int rank = 1;
        for (Athlete curLifter : sortedList) {
            final String curGender = curLifter.getGender();
            // final Integer curAgeGroup = curLifter.getAgeGroup();
            if (!equals(curGender, prevGender)
            // || !equals(curAgeGroup,prevAgeGroup)
            ) {
                // category boundary has been crossed
                rank = 1;
            }

            if (curLifter.isInvited() || !curLifter.getTeamMember()) {
                logger.trace("invited {}  {}rank={} total={} {}",
                        new Object[] { curLifter, rankingType, -1, curLifter.getTotal(), curLifter.isInvited() }); //$NON-NLS-1$
                setRank(curLifter, -1, rankingType);
                setPoints(curLifter, 0, rankingType);
            } else {
                setTeamRank(curLifter, 0, rankingType);
                final double rankingTotal = getRankingTotal(curLifter, rankingType);
                if (rankingTotal > 0) {
                    setRank(curLifter, rank, rankingType);
                    logger.trace("Athlete {}  {}rank={} {}={} total={}",
                            new Object[] { curLifter, rankingType, rank, rankingTotal }); //$NON-NLS-1$
                    rank++;
                } else {
                    logger.trace("Athlete {}  {}rank={} total={}",
                            new Object[] { curLifter, rankingType, 0, rankingTotal }); //$NON-NLS-1$
                    setRank(curLifter, 0, rankingType);
                    rank++;
                }
                final float points = computePoints(curLifter, rankingType);
                setPoints(curLifter, points, rankingType);
            }
            prevGender = curGender;
        }
    }



    /**
     * @param curLifter
     * @param i
     * @param rankingType
     */
    public static void setRank(Athlete curLifter, int i, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            curLifter.setSnatchRank(i);
            break;
        case CLEANJERK:
            curLifter.setCleanJerkRank(i);
            break;
        case TOTAL:
            curLifter.setTotalRank(i);
            break;
        case SINCLAIR:
            curLifter.setSinclairRank(i);
            break;
        case ROBI:
            curLifter.setRobiRank(i);
            break;
        case CUSTOM:
            curLifter.setCustomRank(i);
            break;
		default:
			break;
        }
    }

    /**
     * Assign ranks, sequentially.
     *
     * @param sortedList
     */
    public void assignRanksWithinTeam(List<Athlete> sortedList, Ranking rankingType) {
        String prevTeam = null;
        // String prevAgeGroup = null;
        int rank = 1;
        for (Athlete curLifter : sortedList) {
            final String curTeam = curLifter.getClub() + "_" + curLifter.getGender();
            // final Integer curAgeGroup = curLifter.getAgeGroup();
            if (!equals(curTeam, prevTeam)
            // || !equals(curAgeGroup,prevAgeGroup)
            ) {
                // category boundary has been crossed
                rank = 1;
            }

            if (curLifter.isInvited() || !curLifter.getTeamMember()) {
                setTeamRank(curLifter, -1, rankingType);
            } else {
                if (getRankingTotal(curLifter, rankingType) > 0) {
                    setTeamRank(curLifter, rank, rankingType);
                    rank++;
                } else {
                    setTeamRank(curLifter, 0, rankingType);
                    rank++;
                }
            }
            prevTeam = curTeam;
        }
    }

    /**
     * @param curLifter
     * @param points
     * @param rankingType
     */
    private static void setPoints(Athlete curLifter, float points, Ranking rankingType) {
        logger.trace(curLifter + " " + rankingType + " points=" + points);
        switch (rankingType) {
        case SNATCH:
            curLifter.setSnatchPoints(points);
            break;
        case CLEANJERK:
            curLifter.setCleanJerkPoints(points);
            break;
        case TOTAL:
            curLifter.setTotalPoints(points);
            break;
        case CUSTOM:
            curLifter.setCustomPoints(points);
            break; 
		default:
			break;// computed
        }
    }

    /**
     * @param curLifter
     * @param rankingType
     * @return
     */
    private static float computePoints(Athlete curLifter, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            return pointsFormula(curLifter.getSnatchRank(), curLifter);
        case CLEANJERK:
            return pointsFormula(curLifter.getCleanJerkRank(), curLifter);
        case TOTAL:
            return pointsFormula(curLifter.getTotalRank(), curLifter);
        case CUSTOM:
            return pointsFormula(curLifter.getCustomRank(), curLifter);
        case COMBINED:
            return pointsFormula(curLifter.getSnatchRank(), curLifter)
                    + pointsFormula(curLifter.getCleanJerkRank(), curLifter)
                    + pointsFormula(curLifter.getTotalRank(), curLifter);
		default:
			break;
        }
        return 0;
    }

    /**
     * @param rank
     * @param curLifter
     * @return
     */
    private static float pointsFormula(Integer rank, Athlete curLifter) {
        if (rank == null || rank <= 0)
            return 0;
        if (rank == 1)
            return 28;
        if (rank == 2)
            return 25;
        return 26 - rank;
    }

    /**
     * @param curLifter
     * @param i
     * @param rankingType
     */
    public static void setTeamRank(Athlete curLifter, int i, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            curLifter.setTeamSnatchRank(i);
            break;
        case CLEANJERK:
            curLifter.setTeamCleanJerkRank(i);
            break;
        case TOTAL:
            curLifter.setTeamTotalRank(i);
            break;
        case SINCLAIR:
            curLifter.setTeamSinclairRank(i);
            break;
        case ROBI:
            curLifter.setTeamRobiRank(i);
            break;
        case COMBINED:
            return; // there is no combined rank
		default:
			break;
        }
    }

    /**
     * @param curLifter
     * @param rankingType
     * @return
     */
    public static Integer getRank(Athlete curLifter, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            return curLifter.getSnatchRank();
        case CLEANJERK:
            return curLifter.getCleanJerkRank();
        case SINCLAIR:
            return curLifter.getSinclairRank();
        case ROBI:
            return curLifter.getRobiRank();
        case TOTAL:
            return curLifter.getRank();
        case CUSTOM:
            return curLifter.getCustomRank();
		default:
			break;
        }
        return 0;
    }

    /**
     * @param curLifter
     * @param rankingType
     * @return
     */
    private static double getRankingTotal(Athlete curLifter, Ranking rankingType) {
        switch (rankingType) {
        case SNATCH:
            return curLifter.getBestSnatch();
        case CLEANJERK:
            return curLifter.getBestCleanJerk();
        case TOTAL:
            return curLifter.getTotal();
        case SINCLAIR:
            return curLifter.getSinclair();
        case ROBI:
            return curLifter.getRobi();
        case CUSTOM:
            return curLifter.getCustomScore();
        case COMBINED:
            return 0D; // no such thing
        }
        return 0D;
    }

    static private boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null)
            return true;
        if (o1 != null)
            return o1.equals(o2);
        return false; // o1 is null but not o2
    }

    // public Collection<Team> fullResults(List<Athlete> athletes) {
    // resultsOrder(athletes, Ranking.SNATCH);
    // assignCategoryRanksAndPoints(athletes, Ranking.SNATCH);
    // teamPointsOrder(athletes, Ranking.SNATCH);
    // assignRanksWithinTeam(athletes, Ranking.SNATCH);
    //
    // resultsOrder(athletes, Ranking.CLEANJERK);
    // assignCategoryRanksAndPoints(athletes, Ranking.CLEANJERK);
    // teamPointsOrder(athletes, Ranking.CLEANJERK);
    // assignRanksWithinTeam(athletes, Ranking.CLEANJERK);
    //
    // resultsOrder(athletes, Ranking.TOTAL);
    // assignCategoryRanksAndPoints(athletes, Ranking.TOTAL);
    // teamPointsOrder(athletes, Ranking.TOTAL);
    // assignRanksWithinTeam(athletes, Ranking.TOTAL);
    //
    // combinedPointsOrder(athletes, Ranking.COMBINED);
    // assignCategoryRanksAndPoints(athletes, Ranking.COMBINED);
    // teamPointsOrder(athletes, Ranking.COMBINED);
    // assignRanksWithinTeam(athletes, Ranking.COMBINED);
    //
    // resultsOrder(athletes, Ranking.SINCLAIR);
    // assignCategoryRanksAndPoints(athletes, Ranking.SINCLAIR);
    // teamPointsOrder(athletes, Ranking.SINCLAIR);
    // assignRanksWithinTeam(athletes, Ranking.SINCLAIR);
    //
    // HashSet<Team> teams = new HashSet<Team>();
    // return new TreeSet<Team>(teams);
    // }

}
