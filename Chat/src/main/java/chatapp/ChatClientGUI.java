package chatapp;

import javax.swing.*; import java.awt.*; import java.awt.event.*;
import java.io.*; import java.net.*; import java.nio.file.*;

public class ChatClientGUI extends JFrame {
  private JTextArea chatArea; private JTextField inputField; private JTextField targetField;
  private JButton sendButton, fileButton, exitButton, openDownloadsButton;
  private DataInputStream in; private DataOutputStream out; private Socket socket;
  private final String host; private final int port; private final String username;

  public ChatClientGUI(String host,int port,String username) throws Exception{
    super("Chat Client - "+username);
    this.host=host; this.port=port; this.username=username;
    buildUI(); connect();
  }
  private void buildUI(){
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); setSize(680,520); setLocationRelativeTo(null);
    setLayout(new BorderLayout(8,8));
    chatArea=new JTextArea(); chatArea.setEditable(false); chatArea.setLineWrap(true); chatArea.setWrapStyleWord(true);
    add(new JScrollPane(chatArea),BorderLayout.CENTER);
    JPanel top=new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Đến:")); targetField=new JTextField("ALL",14); top.add(targetField);
    openDownloadsButton=new JButton("Mở thư mục downloads"); top.add(openDownloadsButton);
    add(top,BorderLayout.NORTH);
    JPanel bottom=new JPanel(new BorderLayout(8,8));
    inputField=new JTextField(); bottom.add(inputField,BorderLayout.CENTER);
    JPanel btns=new JPanel(new FlowLayout(FlowLayout.RIGHT));
    fileButton=new JButton("Chọn file…"); sendButton=new JButton("Gửi"); exitButton=new JButton("Thoát");
    btns.add(fileButton); btns.add(sendButton); btns.add(exitButton); bottom.add(btns,BorderLayout.EAST);
    add(bottom,BorderLayout.SOUTH);
    sendButton.addActionListener(e->sendMessage()); inputField.addActionListener(e->sendMessage());
    fileButton.addActionListener(e->chooseAndSendFile()); exitButton.addActionListener(e->disconnectAndClose());
    openDownloadsButton.addActionListener(e->openDownloadsFolder());
    addWindowListener(new WindowAdapter(){ @Override public void windowClosing(WindowEvent e){ disconnectAndClose(); }});
  }
  private void connect() throws Exception{
    socket=new Socket(host,port);
    in=new DataInputStream(new BufferedInputStream(socket.getInputStream()));
    out=new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    out.writeUTF(Protocol.USER+" "+username); out.flush();
    appendChat("Đã kết nối tới "+host+":"+port+" với username: "+username);
    Thread reader=new Thread(this::readerLoop,"reader"); reader.setDaemon(true); reader.start();
  }
  private void readerLoop(){
    try{
      while(true){
        String line=in.readUTF(); if(line==null) break;
        if(line.startsWith(Protocol.FILE+" ")){ receiveFile(line.substring((Protocol.FILE+" ").length())); }
        else { appendChat(line); }
      }
    } catch (EOFException|SocketException e){ appendChat("Disconnected."); }
      catch (IOException e){ appendChat("I/O error: "+e.getMessage()); }
      catch (Exception e){ e.printStackTrace(); appendChat("Error: "+e.getMessage()); }
  }
  private void sendMessage(){
    String text=inputField.getText().trim(); if(text.isEmpty()) return;
    String target=targetField.getText().trim();
    try{
      if(target.isEmpty()||target.equalsIgnoreCase("ALL")){
        out.writeUTF(Protocol.MSG+" "+text);
      } else {
        out.writeUTF(Protocol.DM+" "+target+" "+text);
      }
      out.flush(); inputField.setText("");
    } catch (IOException ex){ appendChat("Không thể gửi tin: "+ex.getMessage()); }
  }
  private void chooseAndSendFile(){
    JFileChooser fc=new JFileChooser(); fc.setDialogTitle("Chọn file để gửi");
    int r=fc.showOpenDialog(this); if(r==JFileChooser.APPROVE_OPTION){ sendFileToTarget(fc.getSelectedFile().toPath()); }
  }
  private void sendFileToTarget(Path path){
    String target=targetField.getText().trim(); if(target.isEmpty()) target="ALL";
    if(!Files.exists(path)||!Files.isRegularFile(path)){ appendChat("File không tồn tại: "+path.toAbsolutePath()); return; }
    try{
      String filename=path.getFileName().toString(); byte[] data=Files.readAllBytes(path);
      out.writeUTF(Protocol.FILE+" "+target+" "+filename+" "+data.length); out.flush(); out.write(data); out.flush();
      appendChat("Đã gửi file '"+filename+"' tới "+target+" ("+data.length+" bytes)");
    } catch (SocketException se){ appendChat("Kết nối bị đóng khi đang gửi file."); }
      catch (IOException e){ appendChat("Lỗi gửi file: "+e.getMessage()); }
  }
  private void receiveFile(String header) throws IOException{
    int lastSpace=header.lastIndexOf(' '); if(lastSpace<=0) return;
    String sizeStr=header.substring(lastSpace+1).trim(); String head2=header.substring(0,lastSpace);
    int firstSpace=head2.indexOf(' '); if(firstSpace<=0) return;
    String from=head2.substring(0,firstSpace).trim(); String filename=head2.substring(firstSpace+1).trim();
    int size=Integer.parseInt(sizeStr);
    byte[] buf=new byte[size]; int read=0; while(read<size){ int r=in.read(buf,read,size-read); if(r==-1) throw new EOFException("Unexpected EOF"); read+=r; }
    Path dir=Paths.get("downloads"); Files.createDirectories(dir); Path outPath=dir.resolve(from+"_"+filename); Files.write(outPath,buf);
    appendChat("Đã nhận file từ "+from+": "+outPath.toAbsolutePath());
  }
  private void openDownloadsFolder(){
    try{ Path dir=Paths.get("downloads"); Files.createDirectories(dir);
      if(Desktop.isDesktopSupported()) Desktop.getDesktop().open(dir.toFile());
      else appendChat("Không mở được thư mục trên hệ thống này. Vui lòng mở tay: "+dir.toAbsolutePath());
    } catch(IOException e){ appendChat("Không mở được thư mục downloads: "+e.getMessage()); }
  }
  private void disconnectAndClose(){
    try{ if(out!=null){ out.writeUTF(Protocol.BYE); out.flush(); } } catch(Exception ignored){}
    try{ if(socket!=null) socket.close(); } catch(Exception ignored){}
    dispose();
  }
  private void appendChat(String s){ SwingUtilities.invokeLater(()->{ chatArea.append(s+"\n"); chatArea.setCaretPosition(chatArea.getDocument().getLength()); }); }
}
