/*******************************************************************************
 * Copyright (c) 2009-2021 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.data.xml;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.List;
import java.util.Locale;

import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import app.owlcms.data.agegroup.AgeGroup;
import app.owlcms.data.agegroup.AgeGroupRepository;
import app.owlcms.data.athlete.Athlete;
import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.competition.CompetitionRepository;
import app.owlcms.data.config.Config;
import app.owlcms.data.group.Group;
import app.owlcms.data.group.GroupRepository;
import app.owlcms.data.jpa.JPAService;
import app.owlcms.data.platform.Platform;
import app.owlcms.data.platform.PlatformRepository;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Logger;

public class CompetitionData {

    final static Logger logger = (Logger) LoggerFactory.getLogger(CompetitionData.class);

    private List<AgeGroup> ageGroups;
    private List<Athlete> athletes;
    private List<Group> groups;
    private List<Platform> platforms;
    private Competition competition;
    private Config config;

    public CompetitionData() {
    }

    public InputStream exportData() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        try {
            ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();

            PipedOutputStream out = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream(out);
            new Thread(() -> {
                try {
                    writerWithDefaultPrettyPrinter.writeValue(out, this.fromDatabase());
                    out.flush();
                    out.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }).start();
            return in;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For debugging
     *
     * @return
     */
    public String exportDataAsString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        ObjectWriter writerWithDefaultPrettyPrinter = mapper.writerWithDefaultPrettyPrinter();
        String serialized;
        try {
            serialized = writerWithDefaultPrettyPrinter.writeValueAsString(this.fromDatabase());
            // System.out.println(serialized);
            return serialized;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public CompetitionData fromDatabase() {
        setAgeGroups(AgeGroupRepository.findAll());
        List<Athlete> allAthletes = AthleteRepository.findAll();
        setAthletes(allAthletes);
        setGroups(GroupRepository.findAll());
        setPlatforms(PlatformRepository.findAll());
        setConfig(Config.getCurrent());
        setCompetition(Competition.getCurrent());
        return this;
    }

    public void restore(InputStream inputStream) {
        this.removeAll();


        CompetitionData updated = this.importData(inputStream);
        JPAService.runInTransaction(em -> {
            try {
                Config config = updated.getConfig();

                Config.setCurrent(config);
                Locale defaultLocale = config.getDefaultLocale();
                Translator.reset();
                Translator.setForcedLocale(defaultLocale);

                Competition competition = updated.getCompetition();
                Competition.setCurrent(competition);
                
                for (AgeGroup ag : updated.getAgeGroups()) {
                    em.persist(ag);
                }
                
                for (Group g : updated.getGroups()) {
                    em.merge(g);
                }

                for (Platform p : updated.getPlatforms()) {
                    em.persist(p);
                }
                
                for (Athlete a : updated.getAthletes()) {
                    // defensive programming if import file is corrupt
                    // a.checkParticipations();
                    em.persist(a);
                }

                em.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    private void removeAll() {
        JPAService.runInTransaction(em -> {
            CompetitionRepository.doRemoveAll(em);
            try {
                this.fromDatabase();
                for (Athlete a : this.getAthletes()) {
                    Athlete aX = em.find(Athlete.class, a.getId());
                    if (aX != null)
                        em.remove(aX);
                }
                for (Group g : this.getGroups()) {
                    Group gX = em.find(Group.class, g.getId());
                    if (gX != null)
                        em.remove(gX);
                }
                for (AgeGroup ag : this.getAgeGroups()) {
                    AgeGroup agX = em.find(AgeGroup.class, ag.getId());
                    if (agX != null)
                        em.remove(agX);
                }
                for (Platform p : this.getPlatforms()) {
                    Platform pX = em.find(Platform.class, p.getId());
                    if (pX != null)
                        em.remove(pX);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @JsonProperty(index = 30)
    public List<AgeGroup> getAgeGroups() {
        return ageGroups;
    }

    @JsonProperty(index = 40)
    public List<Athlete> getAthletes() {
        return athletes;
    }

    @JsonProperty(index = 5)
    public Competition getCompetition() {
        return competition;
    }

    @JsonProperty(index = 1)
    public Config getConfig() {
        return config;
    }

    @JsonProperty(index = 20)
    public List<Group> getGroups() {
        return groups;
    }

    @JsonProperty(index = 10)
    public List<Platform> getPlatforms() {
        return platforms;
    }

    public CompetitionData importData(InputStream serialized) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        CompetitionData newData;
        try {
            newData = mapper.readValue(serialized, CompetitionData.class);
            logger.debug("after unmarshall {}", newData.getPlatforms());
            return newData;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CompetitionData importDataFromString(String serialized)
            throws JsonMappingException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        CompetitionData newData = mapper.readValue(serialized, CompetitionData.class);
        // logger.debug("after unmarshall {}", newData.getPlatforms());
        return newData;
    }

    public void setAgeGroups(List<AgeGroup> ageGroups) {
        this.ageGroups = ageGroups;
    }

    public void setAthletes(List<Athlete> athletes) {
        this.athletes = athletes;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    /**
     * @param competition the competition to set
     */
    private void setCompetition(Competition competition) {
        this.competition = competition;
    }

    /**
     * @param config the config to set
     */
    private void setConfig(Config config) {
        this.config = config;
    }
}