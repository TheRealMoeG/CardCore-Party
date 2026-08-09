package com.cardcoreparty.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.client.ui.PluginPanel;

public class CardCorePartyPanel extends PluginPanel
{
    private static final Color BG_TOP = new Color(21, 18, 29);
    private static final Color BG_BOTTOM = new Color(12, 13, 18);
    private static final Color CARD = new Color(31, 29, 39);
    private static final Color CARD_BORDER = new Color(75, 67, 91);
    private static final Color PURPLE = new Color(151, 105, 255);
    private static final Color PURPLE_DARK = new Color(91, 60, 156);
    private static final Color TEXT = new Color(235, 232, 242);
    private static final Color MUTED = new Color(160, 154, 174);
    private static final Color GREEN = new Color(89, 201, 128);
    private static final Color RED = new Color(224, 102, 102);

    private final JLabel tcgStatus = new JLabel("● Waiting for TCG", SwingConstants.CENTER);
    private final JLabel partyStatus = new JLabel("No party joined", SwingConstants.CENTER);
    private final JLabel sharedCount = new JLabel("0", SwingConstants.CENTER);

    private final JTextField partyKey = styledField("Party code");
    private final JTextField search = styledField("Search shared cards");

    private final JPanel members = transparentVertical();
    private final JPanel cards = transparentVertical();

    private List<String> sharedCards = new ArrayList<>();

    private Runnable createAction = () -> {};
    private Consumer<String> joinAction = s -> {};
    private Runnable leaveAction = () -> {};
    private Runnable syncAction = () -> {};

    public CardCorePartyPanel()
    {
        setLayout(new BorderLayout());
        setBorder(null);

        GradientPanel background = new GradientPanel();
        background.setLayout(new BorderLayout());
        background.setBorder(BorderFactory.createEmptyBorder(10, 9, 10, 9));

        JPanel content = transparentVertical();
        content.setBorder(null);

        content.add(buildHeader());
        content.add(Box.createVerticalStrut(10));
        content.add(buildSharedCard());
        content.add(Box.createVerticalStrut(10));
        content.add(buildPartyCard());
        content.add(Box.createVerticalStrut(10));
        content.add(buildMembersCard());
        content.add(Box.createVerticalStrut(10));
        content.add(buildCollectionCard());

        JScrollPane scroll = new JScrollPane(content);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        background.add(scroll, BorderLayout.CENTER);
        add(background, BorderLayout.CENTER);

        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e) { renderCards(); }
            @Override public void removeUpdate(DocumentEvent e) { renderCards(); }
            @Override public void changedUpdate(DocumentEvent e) { renderCards(); }
        });

        update("waiting", false, 0, Collections.emptyList(), Collections.emptyList());
    }

    private JPanel buildHeader()
    {
        JPanel header = transparentVertical();

        JLabel title = new JLabel("CARDCORE", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Shared OSRS TCG unlocks", SwingConstants.CENTER);
        subtitle.setForeground(MUTED);
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 11f));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        tcgStatus.setForeground(MUTED);
        tcgStatus.setFont(tcgStatus.getFont().deriveFont(Font.BOLD, 11f));
        tcgStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);
        header.add(Box.createVerticalStrut(7));
        header.add(tcgStatus);
        return header;
    }

    private JPanel buildSharedCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));

        JLabel label = smallCaps("SHARED UNLOCKS");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        sharedCount.setForeground(TEXT);
        sharedCount.setFont(sharedCount.getFont().deriveFont(Font.BOLD, 25f));
        sharedCount.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel detail = new JLabel("unique cards", SwingConstants.CENTER);
        detail.setForeground(MUTED);
        detail.setFont(detail.getFont().deriveFont(10f));
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(3));
        panel.add(sharedCount);
        panel.add(detail);

        return panel;
    }

    private JPanel buildPartyCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));

        JPanel heading = transparentHorizontal();
        JLabel label = smallCaps("PARTY");
        partyStatus.setForeground(MUTED);
        partyStatus.setFont(partyStatus.getFont().deriveFont(Font.PLAIN, 10f));

        heading.add(label);
        heading.add(Box.createHorizontalGlue());
        heading.add(partyStatus);

        panel.add(heading);
        panel.add(Box.createVerticalStrut(7));

        partyKey.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        panel.add(partyKey);
        panel.add(Box.createVerticalStrut(6));

        JPanel main = transparentGrid(1, 3, 5, 0);
        JButton create = accentButton("Create");
        JButton join = accentButton("Join");
        JButton leave = subtleButton("Leave");
        create.addActionListener(e -> createAction.run());
        join.addActionListener(e -> joinAction.accept(partyKey.getText()));
        leave.addActionListener(e -> leaveAction.run());
        main.add(create);
        main.add(join);
        main.add(leave);
        main.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        panel.add(main);

        panel.add(Box.createVerticalStrut(5));

        JPanel secondary = transparentGrid(1, 2, 5, 0);
        JButton copy = subtleButton("Copy code");
        JButton sync = subtleButton("Sync TCG");
        copy.addActionListener(e -> copyPartyCode());
        sync.addActionListener(e -> syncAction.run());
        secondary.add(copy);
        secondary.add(sync);
        secondary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        panel.add(secondary);

        return panel;
    }

    private JPanel buildMembersCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));

        panel.add(smallCaps("MEMBERS"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(members);

        return panel;
    }

    private JPanel buildCollectionCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 10, 10, 10));

        JPanel heading = transparentHorizontal();
        heading.add(smallCaps("SHARED CARDS"));
        heading.add(Box.createHorizontalGlue());
        panel.add(heading);
        panel.add(Box.createVerticalStrut(6));

        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        panel.add(search);
        panel.add(Box.createVerticalStrut(7));

        JPanel listWrap = transparentVertical();
        listWrap.add(cards);
        panel.add(listWrap);

        return panel;
    }

    public void setActions(
        Runnable createAction,
        Consumer<String> joinAction,
        Runnable leaveAction,
        Runnable syncAction)
    {
        this.createAction = createAction == null ? () -> {} : createAction;
        this.joinAction = joinAction == null ? s -> {} : joinAction;
        this.leaveAction = leaveAction == null ? () -> {} : leaveAction;
        this.syncAction = syncAction == null ? () -> {} : syncAction;
    }

    public void setPartyKey(String key)
    {
        partyKey.setText(key == null ? "" : key);
    }

    public void update(
        String tcgState,
        boolean inParty,
        int sharedUniqueCount,
        List<String> memberLines,
        List<String> sharedCardNames)
    {
        boolean linked = "linked".equalsIgnoreCase(tcgState);
        tcgStatus.setText(linked ? "● TCG connected" : "● Waiting for TCG");
        tcgStatus.setForeground(linked ? GREEN : MUTED);

        partyStatus.setText(inParty ? "Connected" : "Not joined");
        partyStatus.setForeground(inParty ? GREEN : MUTED);

        sharedCount.setText(String.valueOf(sharedUniqueCount));

        refillMembers(memberLines);

        sharedCards = sharedCardNames == null
            ? new ArrayList<>()
            : new ArrayList<>(sharedCardNames);
        sharedCards.sort(String.CASE_INSENSITIVE_ORDER);
        renderCards();

        revalidate();
        repaint();
    }

    private void refillMembers(List<String> lines)
    {
        members.removeAll();
        List<String> safe = lines == null ? Collections.emptyList() : lines;

        if (safe.isEmpty())
        {
            JLabel empty = mutedLabel("No party members yet");
            members.add(empty);
        }
        else
        {
            for (String line : safe)
            {
                JPanel row = transparentHorizontal();
                JLabel bullet = new JLabel("●");
                bullet.setForeground(GREEN);
                bullet.setFont(bullet.getFont().deriveFont(9f));

                JLabel text = new JLabel(line);
                text.setForeground(TEXT);
                text.setFont(text.getFont().deriveFont(11f));

                row.add(bullet);
                row.add(Box.createHorizontalStrut(6));
                row.add(text);
                row.add(Box.createHorizontalGlue());
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                members.add(row);
            }
        }

        members.revalidate();
        members.repaint();
    }

    private void renderCards()
    {
        cards.removeAll();

        String needle = search.getText() == null ? "" : search.getText().trim().toLowerCase();
        int shown = 0;

        for (String card : sharedCards)
        {
            if (!needle.isEmpty() && !card.toLowerCase().contains(needle))
            {
                continue;
            }

            JLabel item = new JLabel(card);
            item.setForeground(TEXT);
            item.setFont(item.getFont().deriveFont(Font.PLAIN, 11f));
            item.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
            cards.add(item);

            shown++;
            if (shown >= 150)
            {
                cards.add(mutedLabel("More cards available — use search"));
                break;
            }
        }

        if (shown == 0)
        {
            cards.add(mutedLabel(sharedCards.isEmpty() ? "No shared cards yet" : "No matching cards"));
        }

        cards.revalidate();
        cards.repaint();
    }

    private void copyPartyCode()
    {
        String value = partyKey.getText() == null ? "" : partyKey.getText().trim();
        if (!value.isEmpty())
        {
            Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(value), null);
        }
    }

    private static JTextField styledField(String tooltip)
    {
        JTextField field = new JTextField();
        field.setToolTipText(tooltip);
        field.setBackground(new Color(18, 18, 24));
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setSelectionColor(PURPLE_DARK);
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(74, 67, 87)),
            BorderFactory.createEmptyBorder(4, 7, 4, 7)
        ));
        return field;
    }

    private static JButton accentButton(String text)
    {
        return styledButton(text, PURPLE_DARK, new Color(177, 143, 255), TEXT);
    }

    private static JButton subtleButton(String text)
    {
        return styledButton(text, new Color(44, 41, 53), new Color(84, 77, 97), TEXT);
    }

    private static JButton styledButton(String text, Color bg, Color border, Color fg)
    {
        JButton button = new JButton(text);
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border),
            BorderFactory.createEmptyBorder(4, 5, 4, 5)
        ));
        button.setFont(button.getFont().deriveFont(Font.BOLD, 10f));
        return button;
    }

    private static JLabel smallCaps(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(190, 174, 221));
        label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
        return label;
    }

    private static JLabel mutedLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 10f));
        return label;
    }

    private static JPanel transparentVertical()
    {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel transparentHorizontal()
    {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private static JPanel transparentGrid(int rows, int cols, int hgap, int vgap)
    {
        JPanel panel = new JPanel(new GridLayout(rows, cols, hgap, vgap));
        panel.setOpaque(false);
        return panel;
    }

    private static class GradientPanel extends JPanel
    {
        GradientPanel()
        {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setPaint(new GradientPaint(0, 0, BG_TOP, 0, getHeight(), BG_BOTTOM));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // faint CardCore accent glow in the top-right
            g2.setComposite(AlphaComposite.SrcOver.derive(0.10f));
            g2.setColor(PURPLE);
            g2.fillOval(getWidth() - 95, -55, 135, 135);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static class RoundedPanel extends JPanel
    {
        RoundedPanel()
        {
            setOpaque(false);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(CARD);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

            g2.setColor(CARD_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
