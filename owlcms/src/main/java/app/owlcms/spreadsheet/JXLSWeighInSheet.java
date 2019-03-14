/***
 * Copyright (c) 2018-2019 Jean-François Lamy
 * 
 * This software is licensed under the the Apache 2.0 License amended with the
 * Commons Clause.
 * License text at https://github.com/jflamy/owlcms4/master/License
 * See https://redislabs.com/wp-content/uploads/2018/10/Commons-Clause-White-Paper.pdf
 */
package app.owlcms.spreadsheet;

import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.athlete.AthleteRepository;
import app.owlcms.data.athleteSort.AthleteSorter;
import app.owlcms.data.competition.Competition;
import app.owlcms.data.group.Group;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSWeighInSheet extends JXLSWorkbookStreamSource {

    public JXLSWeighInSheet() {
        super(true);
    }

    public JXLSWeighInSheet(boolean excludeNotWeighed) {
        super(excludeNotWeighed);
    }

    Logger logger = LoggerFactory.getLogger(JXLSWeighInSheet.class);

    private Competition competition;

    @Override
    protected void init() {
        super.init();
        competition = Competition.getCurrent();
        getReportingBeans().put("competition", competition);
        getReportingBeans().put("session", getCurrentCompetitionSession());
    }

    @Override
    public InputStream getTemplate() throws IOException {
        String templateName = "/WeighInSheetTemplate_" + UI.getCurrent().getLocale().getLanguage() + ".xls";
        final InputStream resourceAsStream = this.getClass().getResourceAsStream(templateName);
        if (resourceAsStream == null) {
            throw new IOException("resource not found: " + templateName);} //$NON-NLS-1$
        return resourceAsStream;
    }

    @Override
    protected void getSortedAthletes() {
        final Group currentGroup = getCurrentCompetitionSession();
        if (currentGroup != null) {
            // AthleteContainer is used to ensure filtering to current group
            this.athletes = AthleteSorter.displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(currentGroup,isExcludeNotWeighed()));
        } else {
            this.athletes = AthleteSorter.displayOrderCopy(AthleteRepository.findAllByGroupAndWeighIn(null,isExcludeNotWeighed()));
        }
        // AthleteSorter.assignStartNumbers(athletes);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        final Group currentCompetitionSession = getCurrentCompetitionSession();
        if (currentCompetitionSession == null) {
            zapCellPair(workbook, 3, 10);
        }
    }

}
