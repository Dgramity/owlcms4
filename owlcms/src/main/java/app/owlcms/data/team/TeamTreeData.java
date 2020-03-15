/***
 * Copyright (c) 2009-2020 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("Non-Profit OSL" 3.0)
 * License text at https://github.com/jflamy/owlcms4/blob/master/LICENSE.txt
 */
package app.owlcms.data.team;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.flow.data.provider.hierarchy.TreeData;

import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athlete.Gender;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.ui.results.TeamResultsContent;
import app.owlcms.utils.LoggerUtils;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

@SuppressWarnings("serial")
public class TeamTreeData extends TreeData<TeamTreeItem> {

    private final Logger logger = (Logger) LoggerFactory.getLogger(TeamTreeData.class);

    Map<Gender, List<TeamTreeItem>> teamsByGender = new EnumMap<>(Gender.class);

    private List<Group> doneGroups = null;

    private boolean debug = false;

    private Gender genderFilter;

    public TeamTreeData() {
        init();
    }

    private void init() {
        if (debug) {
            logger.setLevel(Level.DEBUG);
        }
        buildTeamItemTree();
        if (debug) {
            dumpTeams();
        }
        for (Gender g : Gender.values()) {
            List<TeamTreeItem> teams = getTeamsByGender().get(g);
            if (teams != null) {
                addItems(teams, TeamTreeItem::getSortedTeamMembers);
            }
        }
    }

    public TeamTreeData(TeamResultsContent teamResultsContent) {
        genderFilter = teamResultsContent.getGenderFilter().getValue();
        init();
    }

    private void buildTeamItemTree() {
        doneGroups = null; // force recompute.
        // TODO bring back mixed using Gender.values()
        for (Gender gender : Gender.mfValues()) {
            if (genderFilter != null && gender != genderFilter) {
                continue;
            }

            logger.debug("**************************************** Gender {} {}", gender, LoggerUtils.whereFrom());

            List<TeamTreeItem> curGenderTeams = getTeamsByGender().get(gender);
            if (curGenderTeams == null) {
                curGenderTeams = new ArrayList<>();
                getTeamsByGender().put(gender, curGenderTeams);
                logger.debug("created list for gender {}: {}", gender, getTeamsByGender().get(gender));
            }

            TeamTreeItem curTeam = null;
            List<Athlete> athletes = (List<Athlete>) Competition.getCurrent().getGlobalTeamsRanking(gender);
            String prevTeamName = null;
            // count points for each team
            for (Athlete a : athletes) {
                // check if competition is a "best n results" team comp.
                // if the competition is "top n", we can have "top 4 men" + "top 2 women", so we want the athlete's
                // gender.
                Integer maxCount = getTopNTeamSize(a.getGender());
                String curTeamName = a.getTeam();
                curTeam = findCurTeam(getTeamsByGender(), gender, curGenderTeams, prevTeamName, curTeam, curTeamName);
                boolean groupIsDone = groupIsDone(a);
                Float curPoints = a.getTotalPoints();

                int curTeamCount = 0;
                logger.debug("Athlete {} {} {} {} {} {}", curTeamName, a, a.getGender(), curPoints, curTeamCount,
                        groupIsDone);
                // results are ordered by total points

                boolean b = curTeamCount < maxCount;
                boolean c = curPoints != null && curPoints > 0;

                if (groupIsDone && b && c) {
                    curTeam.score = curTeam.score + Math.round(curPoints);
                    curTeam.counted += 1;
                }
                curTeam.addTreeItemChild(a, groupIsDone);
                curTeamCount += 1;
                curTeam.size += 1;
                prevTeamName = curTeamName;
            }
        }

//        dumpTrees(teamsByGender);
    }

    private void dumpTeams() {
        for (Gender g : Gender.values()) {
            List<TeamTreeItem> teams = getTeamsByGender().get(g);
            if (teams == null) {
                continue;
            }
            for (TeamTreeItem team : teams) {
                logger.debug("team: {} {}", team.getName(), team.getGender(), team.getScore());
                List<TeamTreeItem> teamMembers = team.getTeamMembers();
                teamMembers.sort(Team.scoreComparator);
                for (TeamTreeItem t : teamMembers) {
                    logger.debug("    {} {}", t.getName(), t.getScore());
                }
            }
        }
    }

    private TeamTreeItem findCurTeam(Map<Gender, List<TeamTreeItem>> teamsByGender, Gender gender,
            List<TeamTreeItem> curGenderTeams, String prevTeamName, TeamTreeItem curTeam, String curTeamName) {
        if (curTeam == null || prevTeamName == null || !curTeamName.contentEquals(prevTeamName)) {
            // maybe we have seen the team already (if mixed)
            TeamTreeItem found = null;
            for (TeamTreeItem ct : curGenderTeams) {
                if (ct.getName() != null && ct.getName().contentEquals(curTeamName)) {
                    found = ct;
                    break;
                }
            }
            if (found != null) {
                curTeam = found;
            } else {
                curTeam = new TeamTreeItem(curTeamName, gender, null, false);
                curTeam.size = AthleteRepository.countTeamMembers(curTeamName, gender);
                teamsByGender.get(gender).add(curTeam);
            }
        }
        return curTeam;
    }

    private Integer getTopNTeamSize(Gender gender) {
        Integer maxCount = null;
        Competition comp = Competition.getCurrent();
        switch (gender) {
        case M:
            maxCount = comp.getMensTeamSize() != null ? comp.getMensTeamSize() : Integer.MAX_VALUE;
            break;
        case F:
            maxCount = comp.getWomensTeamSize() != null ? comp.getWomensTeamSize() : Integer.MAX_VALUE;
            break;
        case MIXED:
            throw new RuntimeException("Can't happen: there is no Top N mixed size");
        }
        return maxCount;
    }

    private boolean groupIsDone(Athlete a) {
        if (doneGroups == null) {
            doneGroups = GroupRepository.findAll().stream().filter(g -> g.isDone()).collect(Collectors.toList());
        }
        return doneGroups.contains(a.getGroup());
    }

    public Map<Gender, List<TeamTreeItem>> getTeamsByGender() {
        return teamsByGender;
    }

}
