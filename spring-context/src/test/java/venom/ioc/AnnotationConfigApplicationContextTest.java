package venom.ioc;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;


public class AnnotationConfigApplicationContextTest {

	@Test
	public void mainFunc() {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(AnnotationConfigApplicationContextTest.class.getPackageName());
		MyBean myBean = ctx.getBean(MyBean.class);
		assertThat(myBean).isNotNull();
		ctx.close();
	}
}