package demo1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

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
	//
	int id;
	
	//每一个selector线程有一个队列
	LinkedBlockingDeque<Channel> lbq = new LinkedBlockingDeque<>();
	
	SelectorThreadGroup stg;
//	
//	
//	public void setWorker(SelectorThreadGroup stg) {
//		this.stg = stg;
//	}

	public SelectorThread(int stid,SelectorThreadGroup stg) {
		try {
			this.stg = stg;
			this.id = stid;
			
			
			//epoll_create
			selector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	public void run() {
		//System.out.println("SelectorThread线程：selector id "+ id + "&线程id" + Thread.currentThread().getId() + "&线程名" + Thread.currentThread().getName());
		// TODO Auto-generated method stub
		while(true) {
			try {
				//第一步select
				//相当于调用了多路复用器
				//select 
				//epoll -> epoll_wait
				
				System.out.println("SelectorThread线程：group [" + (stg.type ==1?"bose":"worker" ) +"] & selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] select 阻塞");
				int nums = selector.select();//阻塞 需要后面 wakeup
				System.out.println("SelectorThread线程：group [" + (stg.type ==1?"bose":"worker" ) +"] & selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] select 停止阻塞");
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
							System.out.println("SelectorThread线程：group [" + (stg.type ==1?"bose":"worker" ) +"] & selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] 有链接进来了");
							acceptHandler(key);
							
						}else if (key.isReadable()) {
							System.out.println("SelectorThread线程：group [" + (stg.type ==1?"bose":"worker" ) +"] & selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] 有消息进来了");
							readHandler(key);
						}else if (key.isWritable()) {
							
						}		
					}
					
					
				}else if (nums < 0) {
					//
				}
				
				//第三步 处理task
				//System.out.println("SelectorThread线程：selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] 被唤醒执行task");
				if (!lbq.isEmpty()) {
					Channel c;
					try {
						c = lbq.take();
						if (c instanceof ServerSocketChannel) {
							ServerSocketChannel server = (ServerSocketChannel)c;
						
							System.out.println("server socket（文件描述符） 注册到 group[ "+(stg.type ==1?"bose":"worker" ) + "] & selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] ");
							server.register(selector, SelectionKey.OP_ACCEPT);		
									
						}else if (c instanceof SocketChannel) {
							SocketChannel client = (SocketChannel)c;
							ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
							System.out.println("server socket（文件描述符） 注册到 group[ "+(stg.type ==1?"bose":"worker" ) + "] & selector id ["+ id + "] & 线程id [" + Thread.currentThread().getId() + "] & 线程名 [" + Thread.currentThread().getName()+"] ");
							client.register(selector, SelectionKey.OP_READ, buffer);
							
						}
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			
				
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
			
			//register 到其他的 selector
			stg.choseSeletor(client);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
}