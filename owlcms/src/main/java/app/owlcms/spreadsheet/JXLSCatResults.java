/*******************************************************************************
 * Copyright (c) 2009-2023 Jean-François Lamy
 *
 * Licensed under the Non-Profit Open Software License version 3.0  ("NPOSL-3.0")
 * License text at https://opensource.org/licenses/NPOSL-3.0
 *******************************************************************************/
package app.owlcms.spreadsheet;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;

import app.owlcms.data.category.Category;
import app.owlcms.i18n.Translator;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * @author jflamy
 *
 */
@SuppressWarnings("serial")
public class JXLSCatResults extends JXLSWorkbookStreamSource {

    final private static Logger jexlLogger = (Logger) LoggerFactory.getLogger("org.apache.commons.jexl2.JexlEngine");
    final private static Logger logger = (Logger) LoggerFactory.getLogger(JXLSCatResults.class);
    final private static Logger tagLogger = (Logger) LoggerFactory.getLogger("net.sf.jxls.tag.ForEachTag");
    static {
        logger.setLevel(Level.INFO);
        jexlLogger.setLevel(Level.ERROR);
        tagLogger.setLevel(Level.ERROR);
    }

//    private byte[] protocolTemplate;

    public JXLSCatResults(UI ui) {
        super();
    }

    public void fix(Workbook workbook, int rownum, int cellnum, String value) {
        Row row = workbook.getSheetAt(0).getRow(rownum);
        final Cell cellLeft = row.getCell(cellnum);
        if (cellLeft == null) {
            return;
        }

        cellLeft.setCellValue(Translator.translate("Category"));

        Cell cellRight = row.getCell(cellnum + 1);
        if (cellRight == null) {
            return;
        }

        cellRight.setCellValue(value);
    }

    @Override
    public void zapCellPair(Workbook workbook, int rownum, int cellnum) {
        Row row = workbook.getSheetAt(0).getRow(rownum);
        final Cell cellLeft = row.getCell(cellnum);
        if (cellLeft == null) {
            return;
        }

        cellLeft.setCellValue("");

        Cell cellRight = row.getCell(cellnum + 1);
        if (cellRight == null) {
            return;
        }

        cellRight.setCellValue("");

        CellStyle blank = workbook.createCellStyle();
        blank.setBorderBottom(BorderStyle.NONE);
        cellLeft.setCellStyle(blank);
        cellRight.setCellStyle(blank);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.concordiainternational.competition.spreadsheet.JXLSWorkbookStreamSource#
     * postProcess(org.apache.poi.ss.usermodel.Workbook)
     */
    @Override
    protected void postProcess(Workbook workbook) {
        final Category cat = getCategory();
        if (cat == null) {
            zapCellPair(workbook, 3, 9);
        } else {
            fix(workbook, 3, 9, cat.toString());
        }
    }
}
