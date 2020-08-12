package demo1;

import demo1.SelectorThreadGroup;

public class MainThread {
	public static void main(String[] args) {
		//这里不做关于IO 和业务的事情
		
		//第一步创建io thread （一个或者多个）
		//混杂模式 ： 只有一个selector可以注册接收，全部都可以注册传输
		//boss worker ：只有boss 可以注册接收，worker才可以注册传输
		
		//管理selector线程组的工作交给了SelectorThreadGroup类
		
		System.out.println("mainThread线程："+ Thread.currentThread().getId() + "&" + Thread.currentThread().getName());
//		SelectorThreadGroup stg = new SelectorThreadGroup(3);
//				
//		//第二步骤 把监听的server注册到某一个selector上，有可能监听多个接口
//		stg.bind(9092);
//		
		
		SelectorThreadGroup boss = new SelectorThreadGroup(3,1);
		boss.setMode(2);//boss worker 模式
		boss.setWorker(new SelectorThreadGroup(3,2));
//		
		boss.bind(9093);
		boss.bind(9094);
		boss.bind(9095);		
	
	}
	

}
