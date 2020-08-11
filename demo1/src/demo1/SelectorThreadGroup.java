package demo1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * 管理selector线程
 *
 */
public class SelectorThreadGroup {
	
	//组里有那些成员？
	SelectorThread[] sts;
	//serverSocket 接收链接的socket
	ServerSocketChannel server = null;
	//线程安全
	AtomicInteger xid = new AtomicInteger(0);
	
	

	
	/**
	 * 
	 * @param i selector 线程数量
	 */
	public SelectorThreadGroup(int num) {
		
		
		sts = new SelectorThread[num];
		//new出num个selector线程
		for (int i = 0; i < num; i++) {
			sts[i] = new SelectorThread();
			//启动线程
			new Thread(sts[i]).start();
		}
		
		// TODO Auto-generated constructor stub
	}

	//选择selector
	//无论是链接还是传输都需要选择一个selector注册
	//无论是serversocket 还是 socket都需要选择一个selector注册
	//ServerSocketChannel 和 SocketChannel 都继承 Channel
	
	public void choseSeletor(Channel c) {
		SelectorThread st = choseFunV1();
		
		//重点
		ServerSocketChannel s = (ServerSocketChannel)c;
		try {
			//selector.wakeup的作用是唤醒selector.select方法,停止阻塞，让其返回。
			
			//第一种情况在register之前唤醒阻塞的select
			//select执行的比wakeup还快，所以还没有注册成功又调用了第二次的select
			//st.selector.wakeup();
			s.register(st.selector, SelectionKey.OP_ACCEPT);
			//第二种情况在register之后唤醒阻塞的select
			//这种情况在selector线程中其实已经阻塞了，你拿到这个线程后调用register还是阻塞在那里，方法执行不到wakeup
			//st.selector.wakeup();
			
			//所以现在需要在线程见进行通信
			
			
			
			
	
			//与主方法调用SelectThread后 《int nums = selector.select();//阻塞 需要后面 wakeup》，因为这里其实已经调用了select方法，还没有注册先select了。
			
		} catch (ClosedChannelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	public SelectorThread choseFunV1() {
		int index = xid.incrementAndGet() % sts.length;
		return sts[index];
	}
	public void choseFunV2() {
		
	}
	
	public void bind(int port) {
		
		try {
			server = ServerSocketChannel.open();
			server.configureBlocking(false);
			server.bind(new InetSocketAddress(port));
			
			//注册到哪里去呢？哪个selector上
			choseSeletor(server);
			
//		selector = selector.open();
//			
//			//select|poll jvm中开辟一个数组把fd4放进去（用户空间）
//			//epoll epoll_ctl(fd3,ADD,fd4,EPOLL_IN,把fd4放到fd3指向的（内核空间）
//			server.register(selector, SelectionKey.OP_ACCEPT);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// TODO Auto-generated method stub
		
	}
	
	
}
