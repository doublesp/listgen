package com.doublesp.coherence.interactors;

import com.doublesp.coherence.R;
import com.doublesp.coherence.interfaces.data.RecipeV2RepositoryInterface;
import com.doublesp.coherence.interfaces.domain.DataStoreInterface;
import com.doublesp.coherence.models.v2.IngredientV2;
import com.doublesp.coherence.models.v2.RecipeV2;
import com.doublesp.coherence.viewmodels.Goal;
import com.doublesp.coherence.viewmodels.Idea;
import com.doublesp.coherence.viewmodels.IdeaMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observer;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

/**
 * Created by pinyaoting on 11/22/16.
 */

public class IngredientInteractor extends IdeaInteractorBase {

    public static final int INGREDIENT_INTERACTOR_BATCH_SIZE = 10;
    public static final long INGREDIENT_INTERACTOR_DEBOUNCE_TIME_IN_MILLIES = 500;
    static final int count = 10;
    static final int offset = 0;

    DataStoreInterface mIdeaDataStore;
    RecipeV2RepositoryInterface mRecipeRepository;
    PublishSubject<String> mSearchDebouncer;

    public IngredientInteractor(DataStoreInterface ideaDataStore,
                                RecipeV2RepositoryInterface recipeRepository) {
        super(ideaDataStore);
        mIdeaDataStore = ideaDataStore;
        mRecipeRepository = recipeRepository;
        mRecipeRepository.subscribeAutoCompleteIngredient(new Observer<List<IngredientV2>>() {
            List<IngredientV2> mIngredients = new ArrayList<IngredientV2>();
            @Override
            public void onCompleted() {
                List<Idea> suggestions = new ArrayList<Idea>();
                for (IngredientV2 ingredient : mIngredients) {
                    Idea idea = new Idea("", R.id.idea_category_recipe_v2, ingredient.getName(), false, R.id.idea_type_suggestion, new IdeaMeta(ingredient.getImage(), ingredient.getName(), null));
                    suggestions.add(idea);
                }
                mIdeaDataStore.setSuggestions(suggestions);
                mIdeaDataStore.setSuggestionState(R.id.state_loaded);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<IngredientV2> ingredientV2s) {
                mIngredients.clear();
                mIngredients.addAll(ingredientV2s);
            }
        });

        mRecipeRepository.subscribeDetail(new Observer<RecipeV2>() {
            RecipeV2 mRecipe;

            @Override
            public void onCompleted() {
                List<Idea> ideas = new ArrayList<Idea>();
                for (IngredientV2 ingredient : mRecipe.getExtendedIngredients()) {
                    Idea idea = new Idea(ingredient.getId(), R.id.idea_category_recipe_v2, ingredient.getName(), false, R.id.idea_type_user_generated, new IdeaMeta(ingredient.getImage(), ingredient.getName(), ingredient.getOriginalString()));
                    ideas.add(idea);
                }
                mIdeaDataStore.setIdeas(ideas);
                mIdeaDataStore.setIdeaState(R.id.state_loaded);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(RecipeV2 recipeV2) {
                mRecipe = recipeV2;
            }
        });
    }

    @Override
    public void acceptSuggestedIdeaAtPos(int pos) {
        mIdeaDataStore.setIdeaState(R.id.state_refreshing);
        Idea idea = mIdeaDataStore.getIdeaAtPos(pos);
        mIdeaDataStore.removeIdea(pos);
        mIdeaDataStore.addIdea(idea);
    }

    @Override
    int getCategory() {
        return R.id.idea_category_recipe_v2;
    }

    @Override
    public void getSuggestions(String keyword) {
        if (keyword == null) {
            // TODO: show default suggestions
            return;
        }
        mIdeaDataStore.setSuggestionState(R.id.state_refreshing);
        searchIngredientsWithDebounce(keyword);
    }

    @Override
    public void loadIdeasFromGoal(Goal goal) {
        mIdeaDataStore.setIdeaState(R.id.state_refreshing);
        mRecipeRepository.searchRecipeDetail(goal.getId());
    }

    private PublishSubject getDebouncer() {
        if (mSearchDebouncer == null) {
            mSearchDebouncer = PublishSubject.create();
            mSearchDebouncer.debounce(INGREDIENT_INTERACTOR_DEBOUNCE_TIME_IN_MILLIES,
                    TimeUnit.MILLISECONDS)
                    .subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            mIdeaDataStore.setIdeaState(R.id.state_refreshing);
                            mRecipeRepository.autoCompleteIngredients(s, INGREDIENT_INTERACTOR_BATCH_SIZE);
                        }
                    });
        }
        return mSearchDebouncer;
    }

    private void searchIngredientsWithDebounce(final String keyword) {
        getDebouncer().onNext(keyword);
    }
}
