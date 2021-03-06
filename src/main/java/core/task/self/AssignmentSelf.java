package core.task.self;

import core.Board;
import core.alert.Notification;
import core.task.exhibition.AssignmentExhibition;
import core.task.handler.AssignmentHandler;
import core.user.Student;
import core.utils.*;
import proto.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Please note that the assignments-task type does not consider the time differences
 * among date values as other types do.
 * In fact, it assumes all time values to be the beginning of the day.
 * Computation with this is easier.
 */
public class AssignmentSelf {
    private String courseName;
    private String question;
    private boolean isGroup;
    private boolean isOn;
    private String modeOfSubmission;
    private String startDate;
    private String deadLine;
    private String dateSubmitted;
    private Timer timer; // the only purpose of its timer is to compare the dates after every day... comparison returns 0 implies deadline is met
    public final ArrayList<String> members = new ArrayList<>(){
        @Override
        public boolean add(String s) {
            groupLabel.setText(getMemberText(1)); // a unit/step behind
            return super.add(s);
        }

        @Override
        public boolean remove(Object o) {
            groupLabel.setText(getMemberText(-1)); // a unit/step ahead
            return super.remove(o);
        }
    };
    public boolean eveIsAlerted;
    public boolean submissionIsAlerted;
    private transient KLabel deadlineIndicator;
    private transient KLabel groupLabel;
    private transient DeadLineEditor deadlineEditor;
    private transient AssignmentExhibition assignmentExhibitor;
    private transient KPanel assignmentPanel;

    // where deadline shall have no time
    public AssignmentSelf(String subject, String deadline, String question, boolean isGroup,
                          String submissionMode){
        this(subject, deadline, question, isGroup, submissionMode, MDate.formatDay(new Date()),
                true);
        setUpUI();
        initializeTimer(Globals.DAY);
    }

    public AssignmentSelf(String subject, String deadline, String question, boolean isGroup,
                          String submissionMode, String startTime, boolean on){
        startDate = startTime;
        courseName = subject;
        this.deadLine = deadline;
        this.question = question;
        this.isGroup = isGroup;
        modeOfSubmission = submissionMode;
        isOn = on;
    }

    private void initializeTimer(int firstDelay){
        timer = new Timer(Globals.DAY,null);
        timer.setInitialDelay(firstDelay);
        timer.addActionListener(e -> {
            if (getTimeRemaining() == 1) {
                signalEveNotice();
            } else if (getTimeRemaining() <= 0) {
                if (assignmentExhibitor != null && assignmentExhibitor.isShowing()) {
                    assignmentExhibitor.dispose();
                }
                if (deadlineEditor != null && deadlineEditor.isShowing()) {
                    deadlineEditor.dispose();
                }
                AssignmentHandler.transferAssignment(this, null, true);
                signalSubmissionNotice();
            }
        });
        timer.start();
    }

    public void setUpUI(){
        deadlineIndicator = new KLabel();
        if (isOn) {
            deadlineIndicator.setText("Deadline: "+deadLine);
            deadlineIndicator.setStyle(FontFactory.createItalicFont(17), Color.RED);
            deadlineIndicator.underline(false);
            deadlineIndicator.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            deadlineIndicator.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    deadlineEditor = new DeadLineEditor(AssignmentSelf.this);
                }
            });
        } else {
            deadlineIndicator.setText("Submitted: "+dateSubmitted);
            deadlineIndicator.setStyle(FontFactory.createPlainFont(16), Color.BLUE);
            deadlineIndicator.setCursor(null);
            for (MouseListener l : deadlineIndicator.getMouseListeners()) {
                deadlineIndicator.removeMouseListener(l);
            }
        }
        deadlineIndicator.setOpaque(false);

        if (isGroup()) {
            groupLabel = KLabel.createIcon("group.png",20,20);
            groupLabel.setText(getMemberText(0));
            groupLabel.setFont(FontFactory.createPlainFont(17));
            groupLabel.setToolTipText("View participants");
            groupLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            groupLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    groupLabel.setForeground(Color.BLUE);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    new MemberExhibitor(AssignmentSelf.this).setVisible(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    groupLabel.setForeground(null);
                }
            });
        } else {
            groupLabel = KLabel.createIcon("personal.png", 20, 20);
            groupLabel.setText("Personal");
        }
        groupLabel.setFont(FontFactory.createPlainFont(16));

        final KButton showButton = KButton.createIconifiedButton("options.png", 15, 15);
        showButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        showButton.addActionListener(e -> assignmentExhibitor = new AssignmentExhibition(AssignmentSelf.this));

        final KPanel quantaPanel = new KPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        quantaPanel.addAll(deadlineIndicator, groupLabel, showButton);

        assignmentPanel = new KPanel(new BorderLayout(), new Dimension(1_000, 35));
        assignmentPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, true));
        assignmentPanel.add(new KPanel(new KLabel(this.getCourseName(), FontFactory.createPlainFont(17),
                Color.BLUE)), BorderLayout.WEST);
        assignmentPanel.add(quantaPanel, BorderLayout.EAST);
    }

    private String getMemberText(int step){
        final int val = members.size() + step;
        if (val <= 0) {
            return "No Members";
        }
        final String text = Globals.checkPlurality(val, "Members");
        return String.format("(%d) %s", val, text.split(" ")[1]);
    }

    private void signalEveNotice(){
        if (!eveIsAlerted) {
            final String text = "<p>"+courseName+ (isGroup ? " Group Assignment" : " Assignment")+" is to be submitted in 24 hours. Submission Mode is "+modeOfSubmission+". " +
                    "If you have already submitted this assignment, mark it as 'submitted' to prevent further-notifications.<p>";
            Notification.create("Assignment Reminder",courseName+" Assignment is due tomorrow!", text);
            eveIsAlerted = true;
        }
    }

    private void signalSubmissionNotice(){
        if (!submissionIsAlerted) {
            final String text = "<p>Submission date for the "+courseName+(isGroup ? " Group Assignment" : " Assignment")+" is past. Submission Mode was "+modeOfSubmission+".</p>";
            Notification.create("Assignment Completed",courseName+" Assignment has reached submission date.", text);
            submissionIsAlerted = true;
        }
    }

    public KLabel getDeadlineIndicator(){
        return deadlineIndicator;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setOn(boolean on) {
        isOn = on;
        if (!on) {
            timer.stop();
        }
    }

    public String getCourseName() {
        return courseName;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDeadLine() {
        return deadLine;
    }

    public void setDeadLine(String deadLine) {
        this.deadLine = deadLine;
        deadlineIndicator.setText("Deadline: "+deadLine);
    }

    public String getStartDate() {
        return startDate;
    }

    public String getSubmissionDate() {
        return dateSubmitted;
    }

    public void setSubmissionDate(String submissionDate) {
        this.dateSubmitted = submissionDate;
    }

    public String getModeOfSubmission() {
        return modeOfSubmission;
    }

    public void setModeOfSubmission(String modeOfSubmission) {
        this.modeOfSubmission = modeOfSubmission;
    }

    public int getTimeRemaining(){
        return (int) MDate.getDifference(
                MDate.parseDay(MDate.formatDay(new Date())), MDate.parseDay(deadLine));
    }

    public KPanel getLayer(){
        return assignmentPanel;
    }

    public void wakeAlive(){
        if (getTimeRemaining() == 1) {
            signalEveNotice();
        }
        final int residue = Globals.DAY - MDate.getTimeValue(new Date());
        initializeTimer(residue);
    }

    public void wakeDead(){
        isOn = false;
        setSubmissionDate(deadLine);
        signalSubmissionNotice();
    }

    public String export() {
        return Globals.joinLines(new Object[]{courseName, MDate.toSerial(MDate.parseDay(deadLine)),
                isGroup, modeOfSubmission, MDate.toSerial(MDate.parseDay(startDate)), isOn,
                MDate.toSerial(MDate.parseDay(dateSubmitted)), eveIsAlerted, submissionIsAlerted});
    }

    private static class MemberExhibitor extends KDialog {
        int pX, pY;
        private KPanel membersPanel;
        private KButton memberAdder;
        private AssignmentSelf assignmentSelf;

        private MemberExhibitor(AssignmentSelf assignmentSelf){
            setUndecorated(true);
            setSize(500, 500);
            setModalityType(KDialog.DEFAULT_MODALITY_TYPE);
            this.assignmentSelf = assignmentSelf;
            final KPanel upperBar = new KPanel(new FlowLayout(FlowLayout.CENTER));
            upperBar.add(new KLabel(assignmentSelf.getCourseName()+" Group Assignment",
                    FontFactory.createPlainFont(15), Color.BLUE));
            upperBar.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                    pX = e.getX();
                    pY = e.getY();
                    upperBar.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    super.mouseDragged(e);
                    setLocation(getLocation().x + e.getX() - pX,
                            getLocation().y + e.getY() - pY);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    super.mouseReleased(e);
                    upperBar.setCursor(null);
                }
            });
            upperBar.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    super.mouseDragged(e);
                    MemberExhibitor.this.setLocation(MemberExhibitor.this.getLocation().x + e.getX() - pX,
                            MemberExhibitor.this.getLocation().y + e.getY() - pY);
                }
            });

            membersPanel = new KPanel(){
                @Override
                public Component add(Component comp) {
                    membersPanel.setPreferredSize(new Dimension(membersPanel.getPreferredSize().width,
                            membersPanel.getPreferredSize().height+35));
                    return super.add(comp);
                }

                @Override
                public void remove(Component comp) {
                    super.remove(comp);
                    membersPanel.setPreferredSize(new Dimension(membersPanel.getPreferredSize().width,
                            membersPanel.getPreferredSize().height-35));
                }
            };
            membersPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            membersPanel.setBackground(Color.WHITE);
            final KScrollPane midScroll = new KScrollPane(membersPanel);

            final KButton closeButton = new KButton("Exit");
            closeButton.addActionListener(e-> dispose());

            memberAdder = new KButton("Add Member");
            memberAdder.addActionListener(e -> {
                final String newMemberName = App.requestInput(this.getRootPane(),
                        "New Member","Enter member's name:\n");
                if (Globals.hasText(newMemberName)) {
                    if (newMemberName.length() > 30) {
                        App.reportError("Error", "Sorry, a member's name cannot exceed 30 characters.");
                    } else if (assignmentSelf.members.contains(newMemberName)) {
                        App.reportError("Error", "'"+newMemberName+"' is already added.");
                    } else {
                        appendNewMember(newMemberName, false);
                        assignmentSelf.members.add(newMemberName);
                    }
                }
            });
            memberAdder.setEnabled(assignmentSelf.isOn);

            final KPanel buttonsPanel = new KPanel(new FlowLayout(FlowLayout.RIGHT, 7, 5));
            buttonsPanel.add(new KPanel(memberAdder), BorderLayout.CENTER);
            buttonsPanel.add(new KPanel(closeButton), BorderLayout.EAST);

            rootPane.setDefaultButton(closeButton);
            final KPanel contentPanel = new KPanel(new BorderLayout());
            contentPanel.setBorder(BorderFactory.createLineBorder(null, 1, false));
            contentPanel.add(upperBar, BorderLayout.NORTH);
            contentPanel.add(midScroll, BorderLayout.CENTER);
            contentPanel.add(buttonsPanel, BorderLayout.SOUTH);
            setContentPane(contentPanel);
            setLocationRelativeTo(Board.getRoot());

            if (assignmentSelf.members.isEmpty()) {
                assignmentSelf.members.add(Student.getFullNamePostOrder());
            }
            appendNewMember(Student.getFullNamePostOrder(), true);
            for (int i = 1; i < assignmentSelf.members.size(); i++) {
                appendNewMember(assignmentSelf.members.get(i), false);
            }
        }

        private void appendNewMember(String name, boolean myself){
            final KLabel nameLabel = new KLabel(myself ? name+" (me)" : name,
                    FontFactory.createPlainFont(18));

            final KButton removeButton = KButton.createIconifiedButton("terminate.png", 17, 17);

            final KPanel namePanel = new KPanel(new BorderLayout(),new Dimension(480,30));
            namePanel.add(new KPanel(nameLabel), BorderLayout.WEST);
            namePanel.add(removeButton, BorderLayout.EAST);
            namePanel.setBackground(Color.WHITE); // except head and toe, the dialog is to be white
            namePanel.getComponent(0).setBackground(Color.WHITE);

            membersPanel.add(namePanel);
            MComponent.ready(membersPanel);

            removeButton.setToolTipText("Remove "+name.split(" ")[0]);
            removeButton.addActionListener(e-> {
                if (App.showYesNoCancelDialog(rootPane, "Confirm",
                        "Are you sure you want to remove '"+name+"' as a participant for this assignment?")) {
                    membersPanel.remove(namePanel);
                    MComponent.ready(membersPanel);
                    assignmentSelf.members.remove(name);
                }
            });
            removeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            removeButton.setEnabled(!myself && assignmentSelf.isOn);
        }
    }

    private static class DeadLineEditor extends KDialog {

        private DeadLineEditor(AssignmentSelf assignmentSelf){
            super("Edit Deadline");
            setResizable(true);
            setModalityType(KDialog.DEFAULT_MODALITY_TYPE);

            final Font valsFont = FontFactory.createPlainFont(16);
            final Date assignmentDeadline = MDate.parseDay(assignmentSelf.deadLine);

            final KTextField dField = KTextField.dayField();
            dField.setText(MDate.getProperty(assignmentDeadline, Calendar.DATE));
            final KTextField mField = KTextField.monthField();
            mField.setText(MDate.getProperty(assignmentDeadline, Calendar.MONTH));
            final KTextField yField = KTextField.yearField();
            yField.setText(MDate.getProperty(assignmentDeadline, Calendar.YEAR));
            final KPanel datesPanel = new KPanel(new FlowLayout(FlowLayout.CENTER));
            datesPanel.addAll(new KLabel("D", valsFont), dField,
                    Box.createRigidArea(new Dimension(20, 30)), new KLabel("M", valsFont),
                    mField, Box.createRigidArea(new Dimension(20, 30)),
                    new KLabel("Y", valsFont), yField);
            final KPanel deadLinePanel = new KPanel(new BorderLayout(), new Dimension(465, 35));
            deadLinePanel.add(new KPanel(new KLabel("New Deadline", FontFactory.createBoldFont(15))),
                    BorderLayout.WEST);
            deadLinePanel.add(datesPanel,BorderLayout.EAST);

            final KButton setButton = new KButton("Set");
            setButton.addActionListener(e1 -> {
                if (Globals.hasNoText(dField.getText())) {
                    App.reportError(rootPane,"Error", "Please specify the day");
                    dField.requestFocusInWindow();
                } else if (Globals.hasNoText(mField.getText())) {
                    App.reportError(rootPane,"Error", "Please specify the month");
                    mField.requestFocusInWindow();
                } else if (Globals.hasNoText(yField.getText())) {
                    App.reportError(rootPane,"Error", "Please specify the year");
                    yField.requestFocusInWindow();
                } else {
                    final Date newDeadline = MDate.date(dField.getTextAsInt(), mField.getTextAsInt(),
                            yField.getTextAsInt(), true);
                    if (newDeadline.before(new Date())) {
                        App.reportError(rootPane, "Invalid Deadline",
                                "That deadline is already past. Enter another date.");
                    } else {
                        assignmentSelf.setDeadLine(MDate.formatDay(newDeadline));
                        dispose();
                    }
                }
            });
            this.getRootPane().setDefaultButton(setButton);
            final KButton cancelButton = new KButton("Cancel");
            cancelButton.addActionListener(e2-> dispose());
            final KPanel bottomPlate = new KPanel(new FlowLayout(FlowLayout.RIGHT));
            bottomPlate.addAll(cancelButton,setButton);

            final KPanel allPlate = new KPanel();
            allPlate.setLayout(new BoxLayout(allPlate, BoxLayout.Y_AXIS));
            allPlate.addAll(deadLinePanel,Box.createVerticalStrut(25),bottomPlate);
            setContentPane(allPlate);
            pack();
            setMinimumSize(getPreferredSize());
            setLocationRelativeTo(Board.getRoot());
            SwingUtilities.invokeLater(()-> this.setVisible(true));
        }
    }

}
