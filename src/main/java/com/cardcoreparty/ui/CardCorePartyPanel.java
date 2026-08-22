package com.cardcoreparty.ui;

import com.cardcoreparty.party.RecentPull;
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
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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

    private static final Font TITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 20);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 12);
    private static final Font BODY_BOLD = new Font(Font.DIALOG, Font.BOLD, 12);
    private static final Font SMALL_FONT = new Font(Font.DIALOG, Font.PLAIN, 10);
    private static final Font SMALL_BOLD = new Font(Font.DIALOG, Font.BOLD, 10);

    private final JLabel tcgStatus = new JLabel("● Waiting for TCG", SwingConstants.CENTER);
    private final JLabel partyStatus = new JLabel("Not joined", SwingConstants.CENTER);
    private final JLabel sharedCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel yoursCount = new JLabel("0", SwingConstants.CENTER);
    private final JLabel partyAddsCount = new JLabel("+0", SwingConstants.CENTER);

    private final JTextField partyKey = styledField("Party code");
    private final JPanel members = transparentVertical();
    private final JPanel partyAvailable = transparentVertical();
    private final JPanel recentPullsPanel = transparentVertical();
    private final JButton browseButton = accentButton("Browse collection");

    private List<String> localCards = new ArrayList<>();
    private List<String> sharedCards = new ArrayList<>();
    private List<String> partyOnlyCards = new ArrayList<>();
    private List<RecentPull> recentPulls = new ArrayList<>();
    private boolean inParty;

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
        content.add(buildHeader());
        content.add(Box.createVerticalStrut(10));
        content.add(buildSharedCard());
        content.add(Box.createVerticalStrut(10));
        content.add(buildPartyCard());
        content.add(Box.createVerticalStrut(10));
        content.add(buildMembersCard());
        content.add(Box.createVerticalStrut(10));
        content.add(buildRecentCard());
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

        browseButton.addActionListener(e -> openCollectionBrowser());
        update("waiting", false, 0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    private JPanel buildHeader()
    {
        JPanel header = transparentVertical();

        JLabel title = new JLabel("CARDCORE", SwingConstants.CENTER);
        title.setForeground(TEXT);
        title.setFont(TITLE_FONT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Shared OSRS TCG unlocks", SwingConstants.CENTER);
        subtitle.setForeground(MUTED);
        subtitle.setFont(SMALL_FONT);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        tcgStatus.setForeground(MUTED);
        tcgStatus.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
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
        panel.setBorder(BorderFactory.createEmptyBorder(9, 10, 10, 10));

        JLabel label = smallCaps("AVAILABLE NOW");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        sharedCount.setForeground(TEXT);
        sharedCount.setFont(new Font(Font.DIALOG, Font.BOLD, 25));
        sharedCount.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel detail = new JLabel("shared unlocks", SwingConstants.CENTER);
        detail.setForeground(MUTED);
        detail.setFont(SMALL_FONT);
        detail.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(3));
        panel.add(sharedCount);
        panel.add(detail);
        panel.add(Box.createVerticalStrut(8));

        JPanel stats = transparentGrid(1, 2, 6, 0);
        stats.add(metric("YOURS", yoursCount));
        stats.add(metric("FROM PARTY", partyAddsCount));
        stats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        panel.add(stats);

        return panel;
    }

    private JPanel buildPartyCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9));

        JPanel heading = transparentHorizontal();
        heading.add(smallCaps("PARTY"));
        heading.add(Box.createHorizontalGlue());

        partyStatus.setForeground(MUTED);
        partyStatus.setFont(SMALL_FONT);
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

    private JPanel buildRecentCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));

        panel.add(smallCaps("RECENT PULLS"));
        panel.add(Box.createVerticalStrut(6));
        panel.add(recentPullsPanel);
        return panel;
    }

    private JPanel buildCollectionCard()
    {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(9, 10, 10, 10));

        JPanel heading = transparentHorizontal();
        heading.add(smallCaps("AVAILABLE FROM PARTY"));
        heading.add(Box.createHorizontalGlue());
        panel.add(heading);
        panel.add(Box.createVerticalStrut(6));

        panel.add(partyAvailable);
        panel.add(Box.createVerticalStrut(8));

        browseButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 29));
        browseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(browseButton);

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
        List<String> localCardNames,
        List<String> sharedCardNames,
        List<RecentPull> recentPulls)
    {
        this.inParty = inParty;

        boolean linked = "linked".equalsIgnoreCase(tcgState);
        tcgStatus.setText(linked ? "● TCG connected" : "● Waiting for TCG");
        tcgStatus.setForeground(linked ? GREEN : MUTED);

        partyStatus.setText(inParty ? "Connected" : "Not joined");
        partyStatus.setForeground(inParty ? GREEN : MUTED);

        localCards = sortedCopy(localCardNames);
        sharedCards = sortedCopy(sharedCardNames);

        Set<String> own = new HashSet<>(localCards);
        partyOnlyCards = new ArrayList<>();
        for (String card : sharedCards)
        {
            if (!own.contains(card))
            {
                partyOnlyCards.add(card);
            }
        }
        partyOnlyCards.sort(String.CASE_INSENSITIVE_ORDER);

        sharedCount.setText(String.valueOf(sharedUniqueCount));
        yoursCount.setText(String.valueOf(localCards.size()));
        partyAddsCount.setText("+" + partyOnlyCards.size());
        browseButton.setText("Browse " + sharedCards.size() + " available");
        this.recentPulls = recentPulls == null ? new ArrayList<>() : new ArrayList<>(recentPulls);

        refillMembers(memberLines);
        refillRecentPulls();
        refillPartyAvailable();

        revalidate();
        repaint();
    }

    private void refillMembers(List<String> lines)
    {
        members.removeAll();
        List<String> safe = lines == null ? Collections.emptyList() : lines;

        if (safe.isEmpty())
        {
            members.add(mutedLabel("No party members yet"));
        }
        else
        {
            for (String line : safe)
            {
                int split = line.lastIndexOf(" — ");
                String playerName = split >= 0 ? line.substring(0, split) : line;
                String count = split >= 0 ? line.substring(split + 3) : "";

                JPanel row = transparentHorizontal();
                JLabel bullet = new JLabel("●");
                bullet.setForeground(GREEN);
                bullet.setFont(SMALL_BOLD);

                JLabel name = new JLabel(playerName);
                name.setForeground(TEXT);
                name.setFont(BODY_FONT);

                JLabel total = new JLabel(count);
                total.setForeground(MUTED);
                total.setFont(SMALL_FONT);

                row.add(bullet);
                row.add(Box.createHorizontalStrut(6));
                row.add(name);
                row.add(Box.createHorizontalGlue());
                row.add(total);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
                members.add(row);
            }
        }

        members.revalidate();
        members.repaint();
    }

    private void refillRecentPulls()
    {
        recentPullsPanel.removeAll();

        if (recentPulls.isEmpty())
        {
            recentPullsPanel.add(mutedLabel("New pulls will appear here"));
        }
        else
        {
            int limit = Math.min(5, recentPulls.size());
            for (int i = 0; i < limit; i++)
            {
                RecentPull pull = recentPulls.get(i);

                JPanel row = transparentVertical();
                JLabel card = new JLabel(pull.getCardName());
                card.setForeground(TEXT);
                card.setFont(BODY_BOLD);

                JLabel meta = new JLabel(pull.getPlayerName() + "  •  " + timeAgo(pull.getTimestamp()));
                meta.setForeground(MUTED);
                meta.setFont(SMALL_FONT);

                row.add(card);
                row.add(Box.createVerticalStrut(1));
                row.add(meta);
                row.setBorder(BorderFactory.createEmptyBorder(2, 0, 5, 0));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
                recentPullsPanel.add(row);
            }
        }

        recentPullsPanel.revalidate();
        recentPullsPanel.repaint();
    }

    private static String timeAgo(long timestamp)
    {
        long seconds = Math.max(0L, (System.currentTimeMillis() - timestamp) / 1000L);
        if (seconds < 60)
        {
            return "just now";
        }

        long minutes = seconds / 60L;
        if (minutes < 60)
        {
            return minutes + "m ago";
        }

        long hours = minutes / 60L;
        if (hours < 24)
        {
            return hours + "h ago";
        }

        return (hours / 24L) + "d ago";
    }

    private void refillPartyAvailable()
    {
        partyAvailable.removeAll();

        if (!inParty)
        {
            partyAvailable.add(mutedLabel("Join a party to share unlocks"));
        }
        else if (partyOnlyCards.isEmpty())
        {
            partyAvailable.add(mutedLabel("No additional party unlocks yet"));
        }
        else
        {
            int limit = Math.min(4, partyOnlyCards.size());
            for (int i = 0; i < limit; i++)
            {
                JPanel row = transparentHorizontal();
                JLabel plus = new JLabel("+");
                plus.setForeground(PURPLE);
                plus.setFont(BODY_BOLD);

                JLabel name = new JLabel(partyOnlyCards.get(i));
                name.setForeground(TEXT);
                name.setFont(BODY_FONT);

                row.add(plus);
                row.add(Box.createHorizontalStrut(6));
                row.add(name);
                row.add(Box.createHorizontalGlue());
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 21));
                partyAvailable.add(row);
            }

            if (partyOnlyCards.size() > limit)
            {
                partyAvailable.add(mutedLabel("+ " + (partyOnlyCards.size() - limit) + " more from your party"));
            }
        }

        partyAvailable.revalidate();
        partyAvailable.repaint();
    }

    private void openCollectionBrowser()
    {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "CardCore Collection");
        dialog.setModal(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        GradientPanel root = new GradientPanel();
        root.setLayout(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel top = transparentVertical();
        JLabel title = new JLabel("SHARED COLLECTION");
        title.setForeground(TEXT);
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
        top.add(title);

        JLabel subtitle = new JLabel(sharedCards.size() + " cards currently available to the party");
        subtitle.setForeground(MUTED);
        subtitle.setFont(BODY_FONT);
        top.add(Box.createVerticalStrut(2));
        top.add(subtitle);
        top.add(Box.createVerticalStrut(10));

        JPanel controls = transparentHorizontal();
        JTextField search = styledField("Search available cards");
        search.setPreferredSize(new Dimension(260, 30));

        JComboBox<String> filter = new JComboBox<>(new String[]{"All available", "Mine", "From party"});
        filter.setPreferredSize(new Dimension(130, 30));
        filter.setBackground(CARD);
        filter.setForeground(TEXT);

        controls.add(search);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(filter);
        top.add(controls);
        root.add(top, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setBackground(BG_BOTTOM);
        list.setForeground(TEXT);
        list.setSelectionBackground(PURPLE_DARK);
        list.setSelectionForeground(TEXT);
        list.setFixedCellHeight(30);

        Set<String> ownSnapshot = new HashSet<>(localCards);
        List<String> allSnapshot = new ArrayList<>(sharedCards);
        List<String> partySnapshot = new ArrayList<>(partyOnlyCards);
        list.setCellRenderer(new CollectionRenderer(ownSnapshot));

        JLabel resultCount = mutedLabel("");

        Runnable refresh = () ->
        {
            String needle = search.getText() == null ? "" : search.getText().trim().toLowerCase();
            int mode = filter.getSelectedIndex();
            List<String> source;
            if (mode == 1)
            {
                source = new ArrayList<>(localCards);
            }
            else if (mode == 2)
            {
                source = partySnapshot;
            }
            else
            {
                source = allSnapshot;
            }

            model.clear();
            for (String card : source)
            {
                if (needle.isEmpty() || card.toLowerCase().contains(needle))
                {
                    model.addElement(card);
                }
            }
            resultCount.setText(model.size() + " shown");
        };

        search.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override public void insertUpdate(DocumentEvent e) { refresh.run(); }
            @Override public void removeUpdate(DocumentEvent e) { refresh.run(); }
            @Override public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        filter.addActionListener(e -> refresh.run());
        refresh.run();

        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(BorderFactory.createLineBorder(CARD_BORDER));
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        root.add(listScroll, BorderLayout.CENTER);

        JPanel footer = transparentHorizontal();
        footer.add(resultCount);
        footer.add(Box.createHorizontalGlue());
        JButton close = subtleButton("Close");
        close.addActionListener(e -> dialog.dispose());
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setSize(520, 620);
        dialog.setMinimumSize(new Dimension(420, 450));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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

    private static List<String> sortedCopy(List<String> input)
    {
        List<String> copy = input == null ? new ArrayList<>() : new ArrayList<>(input);
        copy.sort(String.CASE_INSENSITIVE_ORDER);
        return copy;
    }

    private static JPanel metric(String heading, JLabel value)
    {
        JPanel metric = transparentVertical();
        metric.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(59, 54, 69)),
            BorderFactory.createEmptyBorder(4, 5, 4, 5)
        ));

        JLabel title = new JLabel(heading, SwingConstants.CENTER);
        title.setForeground(MUTED);
        title.setFont(SMALL_BOLD);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        value.setForeground(TEXT);
        value.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        value.setAlignmentX(Component.CENTER_ALIGNMENT);

        metric.add(title);
        metric.add(Box.createVerticalStrut(1));
        metric.add(value);
        return metric;
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
        button.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        return button;
    }

    private static JLabel smallCaps(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(190, 174, 221));
        label.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        return label;
    }

    private static JLabel mutedLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setFont(SMALL_FONT);
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

    private static class CollectionRenderer extends JPanel implements ListCellRenderer<String>
    {
        private final Set<String> owned;
        private final JLabel name = new JLabel();
        private final JLabel source = new JLabel();

        CollectionRenderer(Set<String> owned)
        {
            this.owned = owned;
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            name.setForeground(TEXT);
            name.setFont(BODY_FONT);
            source.setFont(SMALL_BOLD);
            add(name, BorderLayout.CENTER);
            add(source, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends String> list,
            String value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
        {
            boolean mine = owned.contains(value);
            name.setText(value);
            source.setText(mine ? "YOURS" : "PARTY");
            source.setForeground(mine ? MUTED : PURPLE);
            setBackground(isSelected ? PURPLE_DARK : (index % 2 == 0 ? CARD : BG_BOTTOM));
            setOpaque(true);
            return this;
        }
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
