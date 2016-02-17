/* -
 * This program plays a clone of Guitar Hero. The object is to clear notes by
 * holding the corresponding keys when strumming.
 *
 * The default keys are as follows:
 *  1 = Green
 *  2 = Red
 *  3 = Yellow
 *  4 = Blue
 *  5 = Orange
 *  Backspace = Strum
 * 
 * Default scoring:
 * Points are earned for clearing notes using the formula 10 * (notes cleared)^2.
 * Thus, a 1-note chord = 10 points, 2 notes = 40 points, 3 notes = 90 points, etc.
 * Five points deducted for each missed note. Each time you strum an empty note, you
 * will lose 1 point. (Holding down the strum key too long can result in many 1-point 
 * deductions.) You earn additional points each time a streak of cleared chords ends. 
 * You receive 5*(streak length)^2 points. For example, a 4-chord streak would receive 
 * 80 points and an 8-chord streak would receive 320 points. Streak points are added 
 * to your score each time a streak ends.
 *
 * Settings:
 * Key mapping, song selection, background images, delay and scoring can be
 * modifed in the Game Settings instance variable section.
 *
 * @author Jon Cooper (with help from Tyler King and Nick Ledbetter on
 *                     translation guiter tabs to the music data files)
 * @version June 3 2008
 */


import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.sound.sampled.*;
import javax.swing.*;

public class KeyboardHero implements KeyListener
{
	
	/******************************** Game Settings ******************************/
	/*                           (These can be modified)                         */
	
	private String music      = "music/van halen/you really got me.wav"; // path to sound file
	private String tabs       = "music/van halen/you really got me.txt"; // path to tabs file
	private String background = "music/van halen/eddie.jpg";   // path to background image
	
	private int delay = 5; 			    // lower numbers are faster
	private int startAdjustment = 1800; // so music play begins when first chord hits note buttons
	
	private String notes  = "12345";   // keys that correspond to green, red, yellow, blue, and orange
	private char strumKey = (char)(8); // 8 is the ascii code for backspace
	
	private int noteValue  = 100; // base points for each cleared note
	private int missValue  = -5; // deduction for each missed note
	private int emptyValue = -1; // deduction for strumming and empty chord

	private String clank = "sounds/beep.wav"; // path to missed note sound

	/*                                                                           */
	/******************************** End of Settings ****************************/


	private JFrame frame = new JFrame("Keyboard Hero: Legends of Java");
	private Image bgImage = new ImageIcon(background).getImage();;
	private MyDrawPanel drawPanel = new MyDrawPanel();
	private Queue<String> songData = new LinkedList();
	private int width = 800;
	private int height = 630;
	private int score = 0;
	private int streak = 0;
	private int longest = 0;
	private int correctChord;
	private int total = 0;
	private int cleared = 0;
	private double percent = 0;
	private int[] leftX = {350,345,340,335,330,325,320,315,310,305};
	private int[] rightX = {450,455,460,465,470,475,480,485,490,495};
	private int[] y = {200,240,280,320,360,400,440,480,520,560};
	private String myChord = "";
	private String[] chord = new String[y.length];
	private boolean songOver = false;

	
	/**
	 * Game constructor
	 */
	public KeyboardHero() 
	{
		// No code now, however future versions may contain a constructor
		// that takes song data as a parameter(s). 		
	}

	
	/**
	 * Determines notes to be cleared. Also updates streaks and scoring.
	 */
	private void checkChord()
	{
		String rightKeys = chord[correctChord];
		String lettersToRemove = "";
		int hits = 0;
		int misses = 0;
		
		// determine which notes should be cleared
		for (int i=0; i<myChord.length(); i++)
		{
			if (rightKeys.indexOf(myChord.charAt(i))!=-1)
			{
				lettersToRemove += myChord.charAt(i);
			}
		}
		
		// add a miss for every extra note held when strummed
		if (myChord.length()>rightKeys.length())
		{
			misses = myChord.length()-rightKeys.length();
			score += missValue*misses;
		}
		
		// deduct for strumming an empty chord
		if (myChord.length()==0) score += emptyValue;
		
		// count cleared notes and increase score
		hits += lettersToRemove.length();
		cleared += hits;
		score += 10 * hits * hits;
		
		// clear strummed notes from chord
		chord[correctChord] = clearChars(chord[correctChord],lettersToRemove);
		
		// update streak
		if (myChord.length()>0 && chord[correctChord].length()==0 && misses==0)
		{
			streak++;
		}
		else
		{
			endStreak();
		}
	}
	
	
	/**
	 * Deletes chars from string s1 that are contained in s2
	 */
	private String clearChars(String s1, String s2)
	{
		for (int i=0; i<s2.length(); i++)
		{
			String partOne = s1.substring(0,s1.indexOf(s2.charAt(i)));
			String partTwo = s1.substring(s1.indexOf(s2.charAt(i))+1);
			
			s1 = partOne + partTwo;
		}
		
		return s1;		
	}
	
	
	/**
	 * Resets streak to zero and updates longest streak if necessary.
	 */
	private void endStreak()
	{	
		// add streak points
		score += 5*streak*streak;
		
		// update longest streak
		if (streak>longest) longest = streak;
		
		// reset streak to zero
		streak = 0;

		// play missed note sound
		//PlayThread missSound = new PlayThread(clank);
		//missSound.start();
	}

	
	/**
	 * Add pressed keys to myChord and check chord when strummed.
	 */
	public void keyPressed(KeyEvent e)
	{
		char key = e.getKeyChar();
		
		if (myChord.indexOf(key)==-1 && key!=strumKey)
		{
			myChord += key;
		}
		
		if (key==(char)(8))
		{
			checkChord();
		}
	}

	
	/**
	 * Remove released keys from myChord
	 */
	public void keyReleased(KeyEvent e)
	{
		char key = e.getKeyChar();
		
		if (myChord.indexOf(key)!=-1 && key!=(char)(8))
		{
			myChord = clearChars(myChord,key+"");
		}
	}
	

	/**
	 * Does nothing, but necessary to implement KeyListener interface
	 */
	public void keyTyped(KeyEvent e) { /* no code */ }


	/**
	 * Play a game of Keyboard Hero
	 */
	public void play()
	{
		// set window parameters
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(drawPanel);
		frame.setSize(width,height);
		frame.setVisible(true);
		frame.addKeyListener(this);

		// chords start empty
		for (int i=0; i<chord.length; i++) chord[i]="";
		
		// read notes from file
		songData = readFile(tabs);
		
		// start song
		PlayThread song = new PlayThread(music,startAdjustment);
		song.start();
		
		// play the game
		while (!songData.isEmpty())
		{
			for (int i=0; i<y.length; i++)
			{
				// move the notes and frets down 1 pixel
				y[i]++;
				if (y[i]%8==0)
				{
					leftX[i]--;
					rightX[i]++;
				}
				
				// add chord length to note total
				if (y[i]==520)
				{
					total += chord[i].length();
				}

				// determine index of cord that should be strummed
				if (y[i]>520 & y[i]<560)
				{
					correctChord = i;
				}

				// deduct for uncleared notes and end streak
				if (y[i]==561 && chord[i].length()>0)
				{
					score += missValue*chord[i].length();
					endStreak();
				}

				// reset chord and move to top when it reaches bottom of the screen
				if (y[i]==600)
				{
					leftX[i] = 350;
					rightX[i] = 450;
					y[i] = 200;
					chord[i] = songData.remove();
				}
			}
			
			// recalculate percent cleared
			if (total>0) percent = ((int)((double)cleared/(double)total*10000))/100.0;
			
			// refresh graphics
			drawPanel.repaint();
			try
			{
				Thread.sleep(delay);
			} catch(Exception ex) {}
		}
	}
	
	/**
	 * Reads a .txt file line-line-by-line and returns a LinkedList containing
	 * each line of the file
	 */
	public Queue readFile(String filename) 
	{
		Queue<String> lines = new LinkedList();
		
		try 
		{
			BufferedReader in = new BufferedReader(new FileReader(filename));
			while (true) 
			{
				String line = in.readLine();
				if (line == null) break;
			
				// Add each line to the list
				lines.add(line.trim().replace(" ",""));
			}	
			in.close();
		}
		catch (IOException ex) {}
		
		return lines;		
	}
	
			
	/**
	 * Inner class handles music playback.
	 * (Based on AudioPlayer02.java by Richard G. Baldwin)
	 */
	class PlayThread extends Thread
	{
		private AudioFormat audioFormat;
		private AudioInputStream audioInputStream;
		private SourceDataLine sourceDataLine;
		private File soundFile;
		
		private byte tempBuffer[] = new byte[10000];
		private int startDelay = 0;
		private String filePath;
		
		public PlayThread(String f)
		{
			filePath = f;
		}

		public PlayThread(String f, int d)
		{
			filePath = f;
			startDelay = d;
		}
		
	  	public void run()
	  	{
	  		// load the sound file
			try
			{
				soundFile = new File(filePath);
				audioInputStream = AudioSystem.getAudioInputStream(soundFile);
				audioFormat = audioInputStream.getFormat();
				DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class,audioFormat);
				sourceDataLine = (SourceDataLine)AudioSystem.getLine(dataLineInfo);
			}
			catch (Exception e) {}

			// delay music playback till notes are ready
			try
			{
				Thread.sleep(startDelay);
			} catch(Exception ex) {}

			// play the audio file
	    	try
	    	{
				sourceDataLine.open(audioFormat);
				sourceDataLine.start();
				
				int cnt;
				// Keep looping until the input read method returns -1 for empty stream.
				while((cnt = audioInputStream.read(tempBuffer,0,tempBuffer.length)) != -1)
				{
					if(cnt > 0)
					{
					  // Write data to the internal buffer of the data line where it will be delivered to the speaker.
					  sourceDataLine.write(tempBuffer, 0, cnt);
					}//end if
				}
				
				// Block and wait for internal buffer of the data line to empty.
				sourceDataLine.drain();
				sourceDataLine.close();

			}
			catch (Exception ex) {}
		}
	}

	
	/**
	 * Inner class handles game graphics
	 */
	class MyDrawPanel extends JPanel
	{
		public void paintComponent(Graphics g)
		{
			// draw background
			g.setColor(Color.BLACK);
			g.fillRect(0,0,this.getWidth(), this.getHeight());
			g.drawImage(bgImage,0,0,this);
			
			// draw guitar neck
			g.setColor(Color.WHITE);
			g.drawLine(349,200,299,600);
			g.drawLine(350,200,300,600);
			g.drawLine(450,200,500,600);
			g.drawLine(451,200,501,600);

			// draw notes on each visible fret
			for (int i=0; i<y.length; i++)
			{
				int d = (int)((y[i]+140)/34) + 1;
				int h = (int)((double)d*0.7);
				g.setColor(Color.WHITE);
				g.drawLine(leftX[i],y[i]+h/2,rightX[i],y[i]+h/2);

				if (chord[i].indexOf(notes.charAt(0))!=-1)
				{
					g.setColor(Color.GREEN);
					g.fillOval((int)((y[i]-2882.2222)/(-7.5555)),y[i],d,h);
				}
				if (chord[i].indexOf(notes.charAt(1))!=-1)
				{
					g.setColor(Color.RED);
					g.fillOval((int)((y[i]-5300.0000)/(-13.6000)),y[i],d,h);
				}
				if (chord[i].indexOf(notes.charAt(2))!=-1)
				{
					g.setColor(Color.YELLOW);
					g.fillOval(390,y[i],d,h);
				}
				if (chord[i].indexOf(notes.charAt(3))!=-1)
				{
					g.setColor(Color.BLUE);
					g.fillOval((int)((y[i]+9206.6667)/(22.6667)),y[i],d,h);
				}
				if (chord[i].indexOf(notes.charAt(4))!=-1)
				{
					g.setColor(new Color(250,125,0));
					g.fillOval((int)((y[i]+4025.7143)/(9.7143)),y[i],d,h);
				}
				
				// determine cord that should be strummed
				if (y[i]>520 & y[i]<560)
				{
					correctChord = i;
				}
			}
			
			// draw bottom note buttons (filled if corresponding key is pressed)
			g.setColor(Color.GREEN);
			if (myChord.indexOf(notes.charAt(0))!=-1) g.fillOval(310,540,20,14);
			else									  g.drawOval(310,540,20,14);

			g.setColor(Color.RED);
			if (myChord.indexOf(notes.charAt(1))!=-1) g.fillOval(350,540,20,14);
			else									  g.drawOval(350,540,20,14);

			g.setColor(Color.YELLOW);
			
			if (myChord.indexOf(notes.charAt(2))!=-1) g.fillOval(390,540,20,14);
			else									  g.drawOval(390,540,20,14);

			g.setColor(Color.BLUE);
			if (myChord.indexOf(notes.charAt(3))!=-1) g.fillOval(430,540,20,14);
			else								      g.drawOval(430,540,20,14);

			g.setColor(new Color(250,125,0));
			if (myChord.indexOf(notes.charAt(4))!=-1) g.fillOval(470,540,20,14);
			else									  g.drawOval(470,540,20,14);
			
			// display score
			g.setFont(new Font("Sanserif",Font.BOLD,30));
			g.setColor(Color.WHITE);
			g.drawString("Score: " + score,580,360);
			g.setFont(new Font("Sanserif",Font.BOLD,16));
			g.drawString("c: " + cleared,580,400);
			g.drawString("t: " + total,580,420);
			g.drawString("%: " + percent,580,440);
			g.drawString("s: " + streak,580,460);
			g.drawString("l: " + longest,580,480);

			// display song info
			g.setFont(new Font("Sanserif",Font.BOLD,16));
			g.setColor(Color.WHITE);
			g.drawString(music,21,21);
			g.setColor(Color.RED);
			g.drawString(music,20,20);
		}
	}


	/**
	 * main() is where it all begins. rock on, dude!
	 */
	public static void main(String[] args)
	{
		KeyboardHero game = new KeyboardHero();
		game.play();
	}

}