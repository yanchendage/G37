package demo2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class SocketMultipexingSingleThread{
	
	
	
	private ServerSocketChannel server = null;
	//linux 多路复用器（select poll epoll）
	private Selector selector = null;
	int port = 9090;
	
	public void initServer() {
		try {
			//select poll epoll 都需要
			//server 约等于listen状态的fd4
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.bind(new InetSocketAddress(port));
			
			//epoll->epoll_create->fd3（红黑树，链表)
			selector = selector.open();
			
			//select|poll jvm中开辟一个数组把fd4放进去（用户空间）
			//epoll epoll_ctl(fd3,ADD,fd4,EPOLL_IN,把fd4放到fd3指向的（内核空间）
			server.register(selector, SelectionKey.OP_ACCEPT);
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
				
	}
		
	public void start() {
		//初始化服务端，开辟fd4监听文件描述符
		initServer();
		System.out.println("socket 服务器启动了");
		while (true) {
			//多路复用器中注册的文件描述符
			Set<SelectionKey> keys = selector.keys();
			System.out.println("多路复用器中注册了"+ keys.size() +"个socket（文件描述符）");
			try {
				
				//select poll 调用了kernel的select函数,select的参数从jvm中开辟的数组中获取，select(fd4) poll(fd4)
				//epoll 调用了kernel的epoll_wait方法（这里需要注意的是，epool_wait有点懒加载的意思）
				while (selector.select(500) >0) {
					Set<SelectionKey> selectionKeys = selector.selectedKeys();//有状态的fd集合
					Iterator<SelectionKey> iterator = selectionKeys.iterator();
					
					//不论是select poll epoll多路复用器,返回的只是fds,需要自己处理socket的r/w
					//socket分为 serversocket 和 socket （接收链接的socket 和 传输数据的socket）
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						iterator.remove();
						
						//可连接
						if (key.isAcceptable()) {
							//重点
							//调用了kernel函数accept，返回新的文件描述符fd,新的fd放到哪里呢？
							//select poll 模式下，需要将新的fd放到jvm开辟的数组中去,和fd4放到一起
							//epoll模式下调用epoll_ctl把新的fd注册到fd3指向的内核空间中
							
							acceptHandler(key);
						
						//可读的
						}else if (key.isReadable()) {
							//不是删除了注册的文件描述符，只是在内核调用select后得到的有状态的fds中删除
							//key.cancel();
							readHandler(key);
							//在当前线程中，这个方法是阻塞的，因为如果有一个链接数据传输了10年其他的链接早就超时了。
							//所以为什么使用了io threads,io和处理解耦合
							//参考redis tomcat8，9
							
						}else if (key.isWritable()) {
							
						}
					}
					

					
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	
	public void readHandler(SelectionKey key) {
		System.out.println("建立了数据传输");
		//建立channel
		SocketChannel client = (SocketChannel)key.channel();
		//buffer
		ByteBuffer buffer = (ByteBuffer)key.attachment();
		System.out.println(buffer.capacity());
		
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
	
	public void acceptHandler(SelectionKey key) {

		try {
			ServerSocketChannel ssc = (ServerSocketChannel)key.channel();
			//开辟新的fd,假设为fd7
			SocketChannel client = ssc.accept();
			client.configureBlocking(false);
			ByteBuffer buffer = ByteBuffer.allocate(4096);
			
			//select poll 将fd7放到jvm数组中[fd4,fd7]
			//epoll epoll_ctl(fd3,EPOLL_ADD,fd7)
	
			client.register(selector, SelectionKey.OP_READ,buffer);
			
			System.out.println("建立了新的客户端连接："+client.getRemoteAddress());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args) {
		SocketMultipexingSingleThread singleThread = new SocketMultipexingSingleThread();
		singleThread.start();
		
	}
	
}