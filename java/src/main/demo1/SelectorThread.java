package g;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import javax.print.attribute.standard.Severity;
import javax.sql.rowset.serial.SerialArray;

public class SelectorThread implements Runnable{
	/**
	 * 每个线程对应一个selector
	 * 多线程的情况下，该主机，该程序的并发客户端被分配到多个selector上
	 * 注意，每个客户端只绑定到其中一个selector上
	 * 其实不会有交互问题
	 */
	
	//多路复用器
	Selector selector = null;
	
	public SelectorThread() {
		try {
			
			//epoll_create
			selector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	public void run() {
		// TODO Auto-generated method stub
		//loop 转圈
		while(true) {
			try {
				//第一步select
				//相当于调用了多路复用器
				//select 
				//epoll -> epoll_wait
				int nums = selector.select();//阻塞 需要后面 wakeup
				//第二步selectedKeys
				if (nums > 0) {
					//得到所有文件描述符
					Set<SelectionKey> keys = selector.selectedKeys();
					//循环遍历
					Iterator<SelectionKey> iter = keys.iterator();
				
					while (iter.hasNext()) { //线性
						SelectionKey key = iter.next();
						iter.remove();
						
						if (key.isAcceptable()) { //复杂、接受客户端的过程（接收后要注册，多线程下，新的客户端注册到哪里呢？）
							acceptHandler(key);
							
						}else if (key.isReadable()) {
							key.cancel();
							readHandler(key);
							
						}else if (key.isWritable()) {
							
						}		
					}
					
					
				}else if (nums < 0) {
					//
				}
				
				//第三步 处理task
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public void readHandler(SelectionKey key) {
		//buffer
		ByteBuffer buffer = (ByteBuffer)key.attachment();
		//建立channel
		SocketChannel client = (SocketChannel)key.channel();
		
		System.out.println(key);
		buffer.clear();
		

		while (true) {
			try {
				int num = client.read(buffer);
				if (num >0 ) {
					buffer.flip();
					while (buffer.hasRemaining()) {
						client.write(buffer);
					}
					buffer.clear();	
				}else if (num == 0) {
					//读完了，或者没有东西就跳出循环往下执行，要不然阻塞到这里了。
					break;
				}else if(num < 0) {
					//客户端断开了
					System.out.println("客户端" + client.getRemoteAddress()+"断开了");
					key.cancel();//关闭
					break;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}
			
	}
	
	/**
	 * 接受处理器
	 * @param key
	 */
	public void acceptHandler(SelectionKey key) {
		ServerSocketChannel server = (ServerSocketChannel)key.channel();
		try {
			//这一步
			SocketChannel client = server.accept();
			client.configureBlocking(false);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
}
