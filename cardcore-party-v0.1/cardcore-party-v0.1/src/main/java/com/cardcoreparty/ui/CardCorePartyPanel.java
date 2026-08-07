package com.cardcoreparty.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

public class CardCorePartyPanel extends PluginPanel
{
    private final JLabel status = new JLabel("Waiting for OSRS TCG…", SwingConstants.CENTER);
    private final JLabel sharedCount = new JLabel("Shared unique cards: 0", SwingConstants.CENTER);
    private final JPanel members = new JPanel();
    private final JPanel recent = new JPanel();
    private Runnable syncAction = () -> {};
    private java.util.function.Consumer<String> joinAction = s -> {};
    private Runnable createAction = () -> {};
    private Runnable leaveAction = () -> {};
    private final JTextField partyKey = new JTextField();

    public CardCorePartyPanel()
    {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("CardCore Party", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setAlignmentX(CENTER_ALIGNMENT);
        status.setAlignmentX(CENTER_ALIGNMENT);
        sharedCount.setAlignmentX(CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(5));
        header.add(status);
        header.add(sharedCount);

        partyKey.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        partyKey.setToolTipText("CardCore / RuneLite Party passphrase");
        header.add(Box.createVerticalStrut(8));
        header.add(new JLabel("Party passphrase"));
        header.add(partyKey);

        JPanel partyButtons = new JPanel();
        JButton create = new JButton("Create");
        JButton join = new JButton("Join");
        JButton leave = new JButton("Leave");
        create.addActionListener(e -> createAction.run());
        join.addActionListener(e -> joinAction.accept(partyKey.getText()));
        leave.addActionListener(e -> leaveAction.run());
        partyButtons.add(create);
        partyButtons.add(join);
        partyButtons.add(leave);
        header.add(partyButtons);

        JButton sync = new JButton("Sync from OSRS TCG");
        sync.setAlignmentX(CENTER_ALIGNMENT);
        sync.addActionListener(e -> syncAction.run());
        header.add(Box.createVerticalStrut(6));
        header.add(sync);
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        members.setLayout(new BoxLayout(members, BoxLayout.Y_AXIS));
        recent.setLayout(new BoxLayout(recent, BoxLayout.Y_AXIS));

        body.add(sectionLabel("Party collections"));
        body.add(members);
        body.add(Box.createVerticalStrut(10));
        body.add(sectionLabel("Recent unique TCG additions"));
        body.add(recent);

        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(220, 420));
        add(scroll, BorderLayout.CENTER);
    }

    private static JLabel sectionLabel(String text)
    {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    public void setPartyActions(java.util.function.Consumer<String> joinAction, Runnable createAction, Runnable leaveAction)
    {
        this.joinAction = joinAction == null ? s -> {} : joinAction;
        this.createAction = createAction == null ? () -> {} : createAction;
        this.leaveAction = leaveAction == null ? () -> {} : leaveAction;
    }

    public void setPartyKey(String key)
    {
        partyKey.setText(key == null ? "" : key);
    }

    public void setSyncAction(Runnable syncAction)
    {
        this.syncAction = syncAction == null ? () -> {} : syncAction;
    }

    public void update(String statusText, int sharedUniqueCount, List<String> memberLines, List<String> recentLines)
    {
        status.setText(statusText == null ? "" : statusText);
        sharedCount.setText("Shared unique cards: " + sharedUniqueCount);
        refill(members, memberLines, "No party snapshots yet.");
        refill(recent, recentLines, "No new unique cards seen yet.");
        revalidate();
        repaint();
    }

    private static void refill(JPanel panel, List<String> lines, String empty)
    {
        panel.removeAll();
        List<String> safe = lines == null ? Collections.emptyList() : lines;
        if (safe.isEmpty())
        {
            panel.add(new JLabel(empty));
        }
        else
        {
            for (String line : safe)
            {
                panel.add(new JLabel(line));
            }
        }
    }
}
