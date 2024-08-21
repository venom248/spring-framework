package venom.ioc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MyBean {

	@Autowired
	private static MyBeanDependent myBeanDependent;

	public static MyBeanDependent getMyBeanDependent() {
		return myBeanDependent;
	}
}
