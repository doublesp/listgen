package com.doublesp.coherence.dependencies.components.domain;

import com.doublesp.coherence.dependencies.components.data.DataLayerComponent;
import com.doublesp.coherence.interfaces.domain.IdeaInteractorInterface;
import com.doublesp.coherence.interfaces.scopes.DomainLayerScope;
import com.doublesp.coherence.dependencies.modules.domain.DomainLayerModule;

import java.util.Map;

import dagger.Component;

/**
 * Created by pinyaoting on 11/11/16.
 */

@DomainLayerScope
@Component(dependencies = DataLayerComponent.class, modules = DomainLayerModule.class)
public interface DomainLayerComponent {

    Map<Integer, IdeaInteractorInterface> ideaInteractors();

}