package game;
//Group: 	Pelican
//Names: 	Ming Jin, Kangping Xue, Yang Hong, Sharon Stratsianis
//Purpose: 	Main Client that connects to a server and runs the Scrabble GUI

import com.google.gson.Gson;
import common.Client;
import common.SendObject;
import common.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GameClient extends Client {
	
	//Name:			main
	//Description:	entry to start Client, creates the GUI and starts the game
    public static void main(String[] args){
        final JTextField hostInput = new JTextField(30);
        final JTextField portInput = new JTextField(30);
        final JTextField userInput = new JTextField(30);
        JLabel message = new JLabel("Welcome to Scrabble Game!", JLabel.CENTER);
        message.setFont(new Font("Serif", Font.BOLD, 16));
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(0,1,5,5));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.BLACK, 2),
                BorderFactory.createEmptyBorder(6,6,6,6) ));
        inputPanel.add(message);

        hostInput.setText("localhost");
        portInput.setText("12345");
        userInput.setText("your username");

        JPanel row;
        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT));
        row.add(Box.createHorizontalStrut(40));
        row.add(new JLabel("Game Server: "));
        row.add(hostInput);
        inputPanel.add(row);

        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT));
        row.add(Box.createHorizontalStrut(40));
        row.add(new JLabel("Port Number: "));
        row.add(portInput);
        inputPanel.add(row);
        
        row = new JPanel();
        row.setLayout(new FlowLayout(FlowLayout.LEFT));
        row.add(Box.createHorizontalStrut(40));
        row.add(new JLabel("User Name: "));
        row.add(userInput);
        inputPanel.add(row);
        

        while (true){
            int action=JOptionPane.showConfirmDialog(null, inputPanel, "Scrabble Game",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (action != JOptionPane.OK_OPTION)
                return;
            String host;
            host = hostInput.getText().trim();
            int port;
            port = Integer.parseInt(portInput.getText().trim());
            String name;
            name = userInput.getText().trim();
            if (host.length() == 0) {
                message.setText("You must enter a game server!");
                hostInput.requestFocus();
                continue;
            }
            if (name.length() == 0) {
                message.setText("You must enter a game server!");
                hostInput.requestFocus();
                continue;
            }
            try {

                new GameClient(host,port,name);
            }
            catch (IOException e) {
                message.setText("Could not connect to specified host.");
                hostInput.selectAll();
                hostInput.requestFocus();
                continue;
            }
            break;
        }
    }

    private JList<String> playerList; // players
    private GameFrame gameFrame; //frame
    private GameStatus gameStatus; // status of game
    private boolean doSomeThing; //
    private int linenumber=1;
    private boolean gameStart; // does the user want to start the game

    
    //Name: 		    GameClient
    //Description:	All the rules and functions for the Scrabble game
    //				from the client perspective.
    GameClient(String host,int port,String name) throws IOException {
        super(host,port,name);
        playerList = new JList<String>();
        gameFrame=new GameFrame(name);
        doSomeThing=false;
        gameStart=false;
    }

    //Name: 		playerConnected
    //Description:	connect a player to the player list.
    @Override
    protected void playerConnected(int newPlayerID) {
        playerList.removeAll();
        DefaultListModel dlm = new DefaultListModel();
        for(User user:connectedPlayers){
            if(user.id==currenyUser.id||user.isGaming){
                continue;
            }
            dlm.addElement(user.id+"-"+user.name);
        }
        playerList.setModel(dlm);

    }
    
    //Name: 		playerDisconnected
    //Description:	Disconnect a player to the player list.
    @Override
    protected void playerDisconnected(int departingPlayerID) {
        if(gameStart){
            JOptionPane.showMessageDialog(null, "someone has left the game, game will exit now","Quit Game",JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }else{
            playerList.removeAll();
            DefaultListModel dlm = new DefaultListModel();
            for(User user:connectedPlayers){
                if(user.id==currenyUser.id||user.isGaming){
                    continue;
                }
                dlm.addElement(user.id+"-"+user.name);
            }
            playerList.setModel(dlm);
        }
    }

    //Name: 		    messageReceived(String message)
    //Description:	Receive a JSON message
    @Override
    protected void messageReceived(String message) {

        SendObject obj=new Gson().fromJson(message,SendObject.class);
        if(obj.type.equals("InviteBackMessage")){
            InviteBackMessage inviteBackMessage=new Gson().fromJson(message,InviteBackMessage.class);
            gameStatus=inviteBackMessage.state;
            for(User user:gameStatus.players){
                if(user.id==currenyUser.id){
                    SwingUtilities.invokeLater(new Runnable(){
                        public void run() {
                            newState();
                        }
                    });
                    gameStart=true;
                    break;
                }
            }
        }else if(obj.type.equals("GameStatus")){
            gameStatus=new Gson().fromJson(message,GameStatus.class);
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    newState();
                }
            });
        }
    }

    //Name: 		    Next
    //Description:  Next move
    private void Next(){
        gameStatus.cmd="next";
        doSomeThing=false;
        gameStatus.pass = false;
        gameStatus.passNum = 0;
        gameFrame.coord_start=null;
        gameFrame.coord_end=null;
        send(gameStatus);
    }
    
    private void Pass(){
        gameStatus.cmd="next";
        doSomeThing=false;
        gameStatus.pass = true;
        gameStatus.passNum = gameStatus.passNum + 1;
        gameFrame.coord_start=null;
        gameFrame.coord_end=null;
        send(gameStatus);
    }

 
    //Name: 		    newState
    //Description:	update the score and evaluate next state and take action
    private void newState(){
    	boolean flag = false;
    	for (User player:gameStatus.players) {
    		if (currenyUser.id == player.id){
    			flag = true;
    		}
    	}
    	if (!flag) {
    		return;
    	}
        gameFrame.updateUserScoreText();
        char[][] board=gameStatus.board;
        for(int x=0;x<20;x++){
            for(int y=0;y<20;y++){
                char c=board[x][y];
                if(c!=0){
                    gameFrame.buttonBoard[x][y].setText(String.valueOf(c));
                }
            }
        }
        switch (gameStatus.cmd){
            case "null":
            	if(gameStatus.gameEndedInTie){
                    JOptionPane.showMessageDialog(null,"Game Over.");
                    System.exit(0);
                }
                if(currenyUser.id==gameStatus.currentPlayer&&doSomeThing){
                	
                    int result=JOptionPane.showConfirmDialog(null,"is a word here? if there is press 'Yes' and choose the word (top to bottom or left tp right, should be same col or row),is not press 'No' ","Word",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                    if(result==1){
                        if(gameStatus.gameEndedInTie){
                            JOptionPane.showMessageDialog(null,"Game Over.");
                            System.exit(0);
                        }
                        Next();
                    }else{
                        gameFrame.updateInfoWindow("please select start box with letter of the word.");
                    }
                } else if(currenyUser.id!=gameStatus.currentPlayer){
                    if(gameStatus.gameEndedInTie){
                        JOptionPane.showMessageDialog(null,"Game Over.");
                        System.exit(0);
                    }
                }
                if(currenyUser.id==gameStatus.currentPlayer){
                    gameFrame.updateInfoWindow("your turn now");

                }else{
                    for(User user :connectedPlayers){
                        if(gameStatus.currentPlayer==user.id){
                            gameFrame.updateInfoWindow(String.format("[%s]'s turn now", user.name));
                            break;
                        }
                    }

                }

                break;
            case "vote":
                if(currenyUser.id!=gameStatus.currentPlayer){
                    String currentUserName="";
                    for(User user : connectedPlayers){
                        if(user.id==gameStatus.currentPlayer){
                            currentUserName=user.name;
                            break;
                        }
                    }
                    int rlt=JOptionPane.showConfirmDialog(null, String.format("user [%s] select a word [%s], do you agree?",currentUserName,gameStatus.voteStr ),"Vote",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
                    gameStatus.cmd="voteresult";
                    gameStatus.currentVoteRlt=rlt;
                    send(gameStatus);
                }else{
                    gameFrame.updateInfoWindow(String.format("you initiated a vote about if [%s] is a word",gameStatus.voteStr ));
                    gameStatus.cmd="voteresult";
                    gameStatus.currentVoteRlt=0;
                    send(gameStatus);
                }
                break;
            case "voteresult":
                String currentUserName="";
                for(User user : connectedPlayers){
                    if(user.id==gameStatus.voteUser){
                        currentUserName=user.name;
                        break;
                    }
                }
                if(!currenyUser.name.equals(currentUserName)){
                    if(gameStatus.currentVoteRlt==0){
                        gameFrame.updateInfoWindow(String.format("user:[%s] agree [%s] is a word",currentUserName,gameStatus.voteStr ));
                    }else{
                        gameFrame.updateInfoWindow(String.format("user:[%s] do not agree [%s] is a word",currentUserName,gameStatus.voteStr ));
                    }
                }
                if(gameStatus.currentPlayer==currenyUser.id&&gameStatus.players.size()==gameStatus.voteRlt.size()){
                    gameStatus.cmd="votedone";
                    send(gameStatus);
                }
                break;
            case "votedone":
                String currentName="";
                for(User user : connectedPlayers){
                    if(user.id==gameStatus.currentPlayer){
                        currentName=user.name;
                        break;
                    }
                }
                if(gameStatus.currentPlayer==currenyUser.id){
                    if(gameStatus.currentVoteRlt==0){
                        gameFrame.updateInfoWindow(String.format("[%s] is  a word,you gain %d score",gameStatus.voteStr,gameStatus.voteStr.length() ));
                    }else{
                        gameFrame.updateInfoWindow(String.format("[%s] is not a word",gameStatus.voteStr ));
                    }
                }else{
                    if(gameStatus.currentVoteRlt==0){
                        gameFrame.updateInfoWindow(String.format("[%s] is  a word,player;[%s] gain %d score",gameStatus.voteStr,currentName,gameStatus.voteStr.length() ));
                    }else{
                        gameFrame.updateInfoWindow(String.format("[%s] is not a word",gameStatus.voteStr ));
                    }
                }
                Next();
                break;
        }


    }

    //Name: 		    GameFrame
    //Description:	Private class for the GUI 
    private class GameFrame extends JFrame{
        private JLabel InputWord;
        private JTextField wordInput;
        private JButton WordConfirm;
        private WordButton[][] buttonBoard;
        private JDialog SetWordDialog;
        private JPanel btnPanel ;
        private JPanel userPanel;
        private JButton sbtBtn;   // submit button
        private JButton passBtn;  // "pass my turn" button
        private JButton InviteBtn;  // "pass my turn" button
        private JTextArea InfoWindow;
        private JLabel userScoreLabel;
        private Coord coord_start,coord_end,coord_last;

        private InviteDialog inviteDialog;

        //Name:			GameFrame
        //Description:	Initialize the frame.
        public GameFrame(String name){
            super("Scrabble Client v1.0\t\t"+name);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            setLayout(new BorderLayout());

            initButton();
            initUser();
            pack();
            setSize(new Dimension(800, 600));
            setLocationRelativeTo(null);
            setResizable(false);
            setVisible(true);
            coord_last=new Coord();

        }
        
        //Name:			initButton
        //Description:	Initialize the game buttons
        public void initButton() {

            InputWord = new JLabel("Input a word here: ");
            InputWord.setSize(120, 30);
            InputWord.setLocation(20, 40);

            wordInput = new JTextField(10);
            wordInput.setSize(100, 30);
            wordInput.setLocation(150, 40);

            WordConfirm = new JButton("Confirm");
            WordConfirm.setSize(80,30);
            WordConfirm.setLocation(100,100);

            Container container = new Container();
            container.setLayout(new GridLayout(20,20,0,0));
            buttonBoard = new WordButton[20][20];
            for(int i = 0; i < 20; i++){
                for (int j = 0; j < 20; j++) {
                    buttonBoard[i][j] = new WordButton(i,j);
                    container.add(buttonBoard[i][j]);
                }

            }
            add("Center", container);

            btnPanel = new JPanel();
            InviteBtn = new JButton("Invite");
            sbtBtn = new JButton("Submit & Vote");
            passBtn = new JButton("pass");
            
            passBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(!gameStart){
                        updateInfoWindow("please press 'Invite' to start the game'");
                        return;
                    }
                    if(currenyUser.id==gameStatus.currentPlayer){
                    	Pass();
                    }else{
                        gameFrame.updateInfoWindow("not your turn now.");
                    }
                }
            });

            sbtBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(!gameStart){
                        updateInfoWindow("please press 'Invite' to start the game'");
                        return;
                    }
                    if(gameStatus.currentPlayer==currenyUser.id){
                        if(gameFrame.coord_start==null||gameFrame.coord_end==null){
                            gameFrame.updateInfoWindow("please select a word first.");
                        }else{
                            String voteStr=getTheSelectWord();
                            gameStatus.cmd="vote";
                            gameStatus.voteStr=voteStr;
                            send(gameStatus);
                        }
                    }else{
                        gameFrame.updateInfoWindow("not your turn now.");
                    }

                }
            });

            InviteBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(gameStart){
                        return;
                    }
                    inviteDialog=new InviteDialog(null);
                    if(inviteDialog.returnCode!=0){
                        disconnect();
                        System.exit(-1);
                    }
                }
            });

            btnPanel.add(InviteBtn);
            btnPanel.add(sbtBtn);
            btnPanel.add(passBtn);
            add("South",btnPanel);

            SetWordDialog = new JDialog(SetWordDialog, "Word Input Window", true);
            SetWordDialog.setSize(300,200);
            SetWordDialog.setLayout(null);
            SetWordDialog.add(InputWord);
            SetWordDialog.add(wordInput);
            SetWordDialog.add(WordConfirm);
            SetWordDialog.setLocationRelativeTo(null);

            SetWordDialog.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    SetWordDialog.setVisible(false);
                }
            });

            ActionHandler();
        }
        
        //Name:			initUser()
        //Description:	setup the user panel.
        public void initUser() {

            userPanel = new JPanel();
            Box box = new Box(BoxLayout.X_AXIS);
            userScoreLabel=new JLabel(" ");
            box.add(userScoreLabel);

            userPanel.add(box);
            add("North",userPanel);

            InfoWindow = new JTextArea();
            InfoWindow.setEnabled(false);
            InfoWindow.setLineWrap(true);
            InfoWindow.setDisabledTextColor(Color.black);
            InfoWindow.setRows(9999);
            InfoWindow.setPreferredSize(new Dimension(250,600));
            updateInfoWindow("Message Window:");
            JScrollPane jsp = new JScrollPane(InfoWindow);
            jsp.setVerticalScrollBarPolicy(
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            add("East",jsp);
        }
        
        //Name: 		    ActionHandler
        //Decription:	listener for word input.
        public void ActionHandler() {
            WordConfirm.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    String word = wordInput.getText().trim();

                    wordInput.setText("");
                    if(!((word.length()==1)&&((word.charAt(0)>='A'&&word.charAt(0)<='Z')||(word.charAt(0)>='a'&&word.charAt(0)<='z')))){
                        return;
                    }
                    SetWordDialog.setName(word);
                    SetWordDialog.setVisible(false);
                }
            });

        }

        //Name:			updateUserScoreText
        //Description:	Update the users score.
        private void updateUserScoreText(){
            StringBuilder str=new StringBuilder();
            for(User user : gameStatus.players){
                for(User u : connectedPlayers){
                    if(u.id==user.id){
                        str.append(String.format("[%s:%d]-",u.name,user.score ));
                        break;
                    }
                }
            }
            str.deleteCharAt(str.length()-1);
            userScoreLabel.setText(str.toString());
        }

        //Name:			updateInfoWindow
        //Description:	Update the information window.  Display where
        //				we are in the game, action/next action
        private void updateInfoWindow(String msg){
            InfoWindow.append((linenumber++)+": "+msg+"\n");
            InfoWindow.setCaretPosition( InfoWindow.getDocument().getLength());
        }
        
        //Name:			checkButtonSpace
        //Description:	check that start and finish has been selected for comparison.
        private boolean checkButtonSpace(int x,int y){
        	boolean flag = false;
            if(x==coord_start.x){
                for(int yy=coord_start.y;yy<=y;yy++){
                    if(gameFrame.buttonBoard[x][yy].getText().length()==0){
                        return false;
                    }
                    //AAA
                    if (coord_last.x == x && coord_last.y == yy) {
                    	flag = true;
                    }
                }
            }else{
                for(int xx=coord_start.x;xx<=x;xx++){
                    if(gameFrame.buttonBoard[xx][y].getText().length()==0){
                        return false;
                    }
                    //AAA
                    if (coord_last.x == xx && coord_last.y == y) {
                    	flag = true;
                    }
                }
            }
            if (!flag) {
            	return false;
            }
            return true;
        }


        //Name:			getTheSelectWord
        //Description:	return the word selected for voting.
        private String getTheSelectWord(){
            StringBuilder str=new StringBuilder();
            if(coord_end.x==coord_start.x){
                for(int yy=coord_start.y;yy<=coord_end.y;yy++){
                    str.append(gameFrame.buttonBoard[coord_end.x][yy].getText());
                }
            }else{
                for(int xx=coord_start.x;xx<=coord_end.x;xx++){
                    str.append(gameFrame.buttonBoard[xx][coord_end.y].getText());
                }
            }
            return str.toString();
        }

        //Class WordButton
        class WordButton extends JButton{
            private int x,y;
            
            //Name: 	Constructor for WordButton
            WordButton(int x,int y){
                super();
                this.x=x;
                this.y=y;
                this.setMargin(new Insets(0,0,0,0));

                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if(!gameStart){
                            updateInfoWindow("please press 'Invite' to start the game'");
                            return;
                        }
                        if(gameStatus.currentPlayer==currenyUser.id){
                        	// if doSomeThing is true, then i need to select the word
                            if(doSomeThing){
                                if(getText().length()==0){
                                    updateInfoWindow("you have put a letter this turn.");
                                    updateInfoWindow("please select start box with letter of the word.");
                                }else{
                                    if(coord_start==null){
                                        coord_start=new Coord(x,y);
                                        updateInfoWindow("please select end box with letter of the word.");
                                    }else if(coord_end==null){
                                        if((x==coord_start.x||y==coord_start.y)&&(x>=coord_start.x&&y>=coord_start.y)){
                                            if(!checkButtonSpace(x,y)){
                                                coord_start=null;
                                                coord_end=null;
                                                updateInfoWindow("there is something wrong on what you selected, please select start box with letter of the word again. press 'pass' to skip this turn.");
                                            }else{
                                                coord_end=new Coord(x,y);
                                                String str=getTheSelectWord();
                                                updateInfoWindow(String.format("the word you select is [%s],press '%s' to vote ",str,"Submit & Vote" ));
                                            }
                                        }else{
                                            updateInfoWindow("please select from top to bottom or from left to right with same col or row.");
                                            updateInfoWindow("please select start box with letter of the word again. press 'pass' to skip this turn");
                                            coord_start=null;
                                        }
                                    }
                                }
                                return;
                            }
                            if(getText().length()==0){
                                SetWordDialog.setVisible(true);
                                if(SetWordDialog.getName().length()==1){
                                    coord_last.x=x;coord_last.y=y;
                                    gameStatus.board[x][y]=SetWordDialog.getName().toUpperCase().charAt(0);
                                    SetWordDialog.setName("");
                                    doSomeThing=true;
                                    send(gameStatus);
                                }
                            }else{
                                updateInfoWindow("you can only put letter on empty box");
                            }
                        }else{
                            updateInfoWindow("not your turn now");
                        }
                    }
                });
            }
        }
    }

    //Name:			private class InviteDialog
    //Description:	Frame for inviting players to play the game.
    private class InviteDialog{
        private JDialog dialog;
        private int returnCode;

        InviteDialog(Frame frame){
            returnCode=-1;
            GridBagConstraints c=new GridBagConstraints();
            c.fill=GridBagConstraints.BOTH;

            GridBagLayout gridBagLayout=new GridBagLayout();

            JPanel panel=new JPanel();
            panel.setLayout(gridBagLayout);
            JScrollPane invitePanel=new JScrollPane();
            invitePanel.setViewportView(playerList);
            c.gridx=0;
            c.gridy=0;
            c.gridheight=5;
            c.gridwidth=1;
            c.ipady=180;
            gridBagLayout.setConstraints(invitePanel,c);
            panel.add(invitePanel);
            JButton comfirmButton=new JButton("Invite");
            JButton cancelButton=new JButton("Cancel");
            comfirmButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    List<String> sel = playerList.getSelectedValuesList();
                    ArrayList<Integer> players=new ArrayList<>(sel.size());
                    for (String str : sel){
                        players.add(Integer.valueOf(str.split("-")[0]));
                    }
                    send(new InviteMessage(currenyUser.id,players));
                    returnCode=0;
                    dialog.setVisible(false);
                }
            });

            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                }
            });

            JPanel buttonPanel=new JPanel();

            buttonPanel.add(comfirmButton);
            buttonPanel.add(cancelButton);

            c.gridx=0;
            c.gridy=6;
            c.ipady=20;
            c.gridheight=1;
            gridBagLayout.setConstraints(buttonPanel,c);

            panel.add(buttonPanel);
            dialog =new JDialog(frame,"Invite to Game",true);
            dialog.setContentPane(panel);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.setModal(true);
            dialog.setLocationRelativeTo(null);
            dialog.setSize(300,300);
            dialog.setVisible(true);
        }
    }
}
