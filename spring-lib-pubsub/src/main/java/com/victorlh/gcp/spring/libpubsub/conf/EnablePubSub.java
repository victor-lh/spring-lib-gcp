

package com.victorlh.gcp.spring.libpubsub.conf;


import com.victorlh.gcp.spring.libcore.conf.GCPCredentialsConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({PubSubConfigurationSelector.class, GCPCredentialsConfiguration.class})
public @interface EnablePubSub {
}
