package demo1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.ls.LSException;

/**
 * 
 * 管理selector线程
 *
 */
public class SelectorThreadGroup {
	
	//1boss 2worker
	int type =1;
	//1模式混杂模式 or 2boss&worker模式
	int mode =1;
	
	//当前组内的成员
	SelectorThread[] sts;
	//serverSocket 接收链接的socket
	ServerSocketChannel server = null;
	//线程安全
	AtomicInteger xid = new AtomicInteger(0);public SelectorThreadGroup() {
		// TODO Auto-generated constructor stub
	}
	//当前组内的其他组
	//如果为混杂模式当前的组既是boss又是worker
	SelectorThreadGroup wg = this;
	
	/**
	 * 设置worker组
	 * @param stg
	 */
	public void setWorker(SelectorThreadGroup stg) {
		this.wg = stg;
	}
	
	public void setMode(int mode) {
		this.mode = mode;
		
		if (mode == 1) {
			
		}
		System.out.println("当前模式为：" + (mode ==1?"混杂":"boss worker" ));
	}
	
	/**
	 * 
	 * @param i selector 线程数量
	 */
	public SelectorThreadGroup(int num, int type) {
		this.type = type;
		
		//System.out.println("SelectorThreadGroup线程：group [" +(mode ==1?"bose":"worker" ) + "]" + Thread.currentThread().getId() +"&"+Thread.currentThread().getName() );
		
		//new 了一个 selector线程数组
		sts = new SelectorThread[num];
		//new出num个selector线程
		for (int i = 0; i < num; i++) {
			sts[i] = new SelectorThread(i, this);
			//启动线程
			new Thread(sts[i]).start();
		}
		
		// TODO Auto-generated constructor stub
	}

	//选择selector
	//无论是链接还是传输都需要选择一个selector注册
	//无论是serversocket 还是 socket都需要选择一个selector注册
	//ServerSocketChannel 和 SocketChannel 都继承 Channel
	
	
	/**
	 * 混杂模式
	 * @param c
	 */
	public void choseSeletor(Channel c) {
		
		//混杂
		if (mode == 1) {
			
			SelectorThread st = choseFunV1();
			//选择selector线程后，把当前的channel传进去
			st.lbq.add(c);
			//打断阻塞
			st.selector.wakeup();
			System.out.println("SelectorThreadGroup线程: wakeup selector id ["+st.id+"]");
			
		}else if (mode == 2) {
			
			if (c instanceof ServerSocketChannel) {
				SelectorThread st = choseFunV1();
				
				//选择selector线程后，把当前的channel传进去
				st.lbq.add(c);
				//打断阻塞
				st.selector.wakeup();
				
				System.out.println("SelectorThreadGroup线程【boss组】: wakeup selector id ["+st.id+"]");
			}else if (c instanceof SocketChannel) {
				SelectorThread st = choseFunV2();
				st.lbq.add(c);
				//打断阻塞
				st.selector.wakeup();
				System.out.println("SelectorThreadGroup线程【worker组】: wakeup selector id ["+st.id+"]");
			}
			
		}
		
		

		
		//重点 
//		ServerSocketChannel s = (ServerSocketChannel)c;
//		try {
//			//selector.wakeup的作用是唤醒selector.select方法,停止阻塞，让其返回。
//			
//			//第一种情况在register之前唤醒阻塞的select
//			//select执行的比wakeup还快，所以还没有注册成功又调用了第二次的select
//			//st.selector.wakeup();
//			s.register(st.selector, SelectionKey.OP_ACCEPT);
//			//第二种情况在register之后唤醒阻塞的select
//			//这种情况在selector线程中其实已经阻塞了，你拿到这个线程后调用register还是阻塞在那里，方法执行不到wakeup
//			//st.selector.wakeup();
//			
//			//所以现在需要在线程内部进行通信
//			//与主方法调用SelectThread后 《int nums = selector.select();//阻塞 需要后面 wakeup》，因为这里其实已经调用了select方法，还没有注册先select了。
//			
//		} catch (ClosedChannelException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}	
	}
	

	/**
	 * 混杂模式
	 * @return
	 */
	public SelectorThread choseFunV1() {
		int index = xid.incrementAndGet() % sts.length;
		
		System.out.println("SelectorThreadGroup线程: 选择的selector id :	[" + index + "]");
		return sts[index];
	}
	
	/**
	 * boss worker模式
	 */
	public SelectorThread choseFunV2() {
		int index = xid.incrementAndGet() % wg.sts.length;
		
		System.out.println("SelectorThreadGroup线程: 选择的selector id :	[" + index + "]");
		return wg.sts[index];
	}
	
	public void bind(int port) {
		
		try {
			System.out.println("SelectorThreadGroup线程 建立server socket（文件描述符）并绑定到"+port+"端口");
			
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
