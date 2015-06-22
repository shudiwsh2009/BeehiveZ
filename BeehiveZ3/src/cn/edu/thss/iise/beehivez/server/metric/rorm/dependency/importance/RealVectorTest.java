package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency.importance;

import java.util.HashSet;
import java.util.Set;



public class RealVectorTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Set<Test> set = new HashSet<Test>();
		Test t1 = new Test(1);
		Test t2 = new Test(2);
		Test t3 = new Test(3);
		t1.t = t2; t2.t = t3; t3.t = t1;
		set.add(t1);
		set.add(t2);
		set.add(t3);
		for(Test t : set) {
			System.out.print(t.in + " ");
			t = t.t;
			System.out.println(t.in);
		}
	}
}

class Test {
	public int in = 0;
	public Test t = null;
	
	public Test(int i) {
		in = i;
	}
}
