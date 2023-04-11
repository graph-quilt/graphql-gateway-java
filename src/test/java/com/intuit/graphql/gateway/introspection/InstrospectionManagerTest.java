package com.intuit.graphql.gateway.introspection;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import com.intuit.graphql.gateway.config.properties.IntrospectionProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class InstrospectionManagerTest {

  private IntrospectionManager subject;

  @Mock
  private IntrospectionProperties propertiesMock;

  @Before
  public void setup() {
    subject = new IntrospectionManager(propertiesMock);
  }

  @Test
  public void isIntrospectionNotEnabled_propertyIsSetToTrue_returnsFalse() {

    when(propertiesMock.isEnabled()).thenReturn(true);

    boolean actual = subject.isIntrospectionNotEnabled();

    assertThat(actual).isFalse();

  }

  @Test
  public void isIntrospectionNotEnabled_propertyIsSetToFalse_returnsTrue() {

    when(propertiesMock.isEnabled()).thenReturn(false);

    boolean actual = subject.isIntrospectionNotEnabled();

    assertThat(actual).isTrue();

  }

}
