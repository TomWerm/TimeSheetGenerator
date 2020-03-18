package io;

import java.time.format.DateTimeFormatter;

import javax.swing.filechooser.FileNameExtensionFilter;

import data.Entry;
import data.TimeSheet;
import data.WorkingArea;

/**
 * The LatexGenerator generates a LaTeX string based on a template and fills it with 
 * information of a {@link TimeSheet} and its associated {@link Entry Entries}.
 */
public class LatexGenerator implements IGenerator {

    private final static String FILE_EXTENSION = "tex";
    private final static String FILE_DESCRIPTION = "TeX-File";
    
    private final static String SHORTHAND_VACATION = "U";
    
    private final TimeSheet timeSheet;
    private final String template;
    
    /**
     * Constructs a new {@link TimeSheet} instance.
     * @param timeSheet - as source of data to fill into the template.
     * @param template - the template the generated LaTeX {@link String} should be based on.
     */
    public LatexGenerator(TimeSheet timeSheet, String template) {
        this.timeSheet = timeSheet;
        this.template = template;
    }

    @Override
    public String generate() {
        String filledTex = template;
        
        /*
         * This loop replaces the document-public placeholder in the TeX template with the correct data.
         */
        for (TimeSheetElement elem : TimeSheetElement.values()) {
            filledTex = filledTex.replace(elem.getPlaceholder(), getSubstitute(timeSheet, elem));
        }
        
        /*
         * This loop replaces the placeholder in the TeX template with the correct data.
         * If the TimeSheet contains to many elements for the table,
         * all rows get filled and the rest of data gets lost.
         */
        for (EntryElement elem : EntryElement.values()) {
            String placeholder = elem.getPlaceholder();
            for (Entry entry : timeSheet.getEntries()) {
                filledTex = filledTex.replaceFirst(placeholder, getSubstitute(entry, elem));
            }
        }
        
        /*
         * This loop fills up all not-needed rows of the table with a blank space.
         * IMPORTANT: Some kind of character is needed to make the TeX compile correctly on some TeX compilers.
         */
        for (EntryElement elem : EntryElement.values()) {
            filledTex = filledTex.replace(elem.getPlaceholder(), "");
        }
         
        return filledTex;
    }

    @Override
    public FileNameExtensionFilter getFileNameExtensionFilter() {
        return new FileNameExtensionFilter(FILE_DESCRIPTION, FILE_EXTENSION);
    }

    /**
     * Returns a substitute coming from a {@link TimeSheet} replacing
     * a placeholder associated with a {@link TimeSheetElement} in the document {@link String}. 
     * @param timeSheet - to get the substitute from
     * @param element - element to get the substitute for
     * @return A substitute as a {@link String}
     */
    private static String getSubstitute(TimeSheet timeSheet, TimeSheetElement element) {
        String value;
        switch (element) {
            case YEAR:
                value =  Integer.toString(timeSheet.getYear());
                break;
            case MONTH:
                value = Integer.toString(timeSheet.getMonth().getValue());
                break;
            case EMPLOYEE_NAME:
                value = timeSheet.getEmployee().getName();
                break;
            case EMPLOYEE_ID:
                value = Integer.toString(timeSheet.getEmployee().getId());
                break;
            case GFUB:
                if (timeSheet.getProfession().getWorkingArea() == WorkingArea.GF) {
                    value = "\\textbf{GF:} $\\boxtimes$ \\textbf{UB:} $\\Box$";
                } else {
                    value = "\\textbf{GF:} $\\Box$ \\textbf{UB:} $\\boxtimes$";
                }
                break;
            case DEPARTMENT:
                value = timeSheet.getProfession().getDepartmentName();
                break;
            case MAX_HOURS:
                value = timeSheet.getProfession().getMaxWorkingTime().toString();
                break;
            case WAGE:
                value = Double.toString(timeSheet.getProfession().getWage());
                break;
            case VACATION:
                value = timeSheet.getTotalVacationTime().toString();
                break;
            case HOURS_SUM:
                value = timeSheet.getTotalWorkTime().add(timeSheet.getTotalVacationTime()).toString();
                break;
            case TRANSFER_PRED:
                value = timeSheet.getPredTransfer().toString();
                break;
            case TRANSFER_SUCC:
                value = timeSheet.getSuccTransfer().toString();
                break;
            default:
                value = null;
                break;
        }
        return value;
    }
    
    /**
     * Returns a substitute coming from an {@link Entry} elements data replacing
     * a placeholder associated with an {@link EntryElement} in the document {@link String}. 
     * @param entry - to get the substitute from
     * @param element - element to get the substitute for
     * @return A substitute as a {@link String}
     */
    private static String getSubstitute(Entry entry, EntryElement element) {
        String value;
        switch (element) {
            case TABLE_ACTION:
                value = entry.getAction();
                break;
            case TABLE_DATE:
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yy");
                value = entry.getDate().format(formatter);
                break;
            case TABLE_START:
                value = entry.getStart().toString();
                break;
            case TABLE_END:
                value = entry.getEnd().toString();
                break;
            case TABLE_PAUSE:
                value = entry.getPause().toString();
                break;
            case TABLE_TIME:
                if (entry.isVacation()) {
                    value = SHORTHAND_VACATION + " " + entry.getWorkingTime().toString();
                } else {
                    value = entry.getWorkingTime().toString();
                }
                break;
            default:
                value = null;
                break;
        }
        return value;
    }
    
    /**
     * The different elements representing the {@link TimeSheet}, especially the 
     * {@link Employee} and {@link Profession}, on the document.
     */
    private static enum TimeSheetElement {
        YEAR("!year"),
        MONTH("!month"),
        EMPLOYEE_NAME("!employeeName"),
        EMPLOYEE_ID("!employeeID"),
        GFUB("!workingArea"),
        DEPARTMENT("!department"),
        MAX_HOURS("!workingTime"),
        WAGE("!wage"),
        VACATION("!vacation"),
        HOURS_SUM("!sum"),
        TRANSFER_PRED("!carryPred"),
        TRANSFER_SUCC("!carrySucc");
        
        private final String placeholder;
        
        private TimeSheetElement(String placeholder) {
            this.placeholder = placeholder;
        }
        
        public String getPlaceholder() {
            return this.placeholder;
        }
    }
    
    /**
     * The different elements representing the {@link Entry entries} on the document.
     */
    private static enum EntryElement {
        TABLE_ACTION("!action"),
        TABLE_DATE("!date"),
        TABLE_START("!begin"),
        TABLE_END("!end"),
        TABLE_PAUSE("!break"),
        TABLE_TIME("!dayTotal");
        
        private final String placeholder;
        
        private EntryElement(String placeholder) {
            this.placeholder = placeholder;
        }
        
        public String getPlaceholder() {
            return this.placeholder;
        }
    }
}
