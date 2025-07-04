package com.example.cookingmanagement.service;

import com.example.cookingmanagement.entity.Ingredient;
import com.example.cookingmanagement.entity.Recipe;
import com.example.cookingmanagement.form.IngredientForm;
import com.example.cookingmanagement.form.RecipeForm;
import com.example.cookingmanagement.mapper.IngredientMapper;
import com.example.cookingmanagement.mapper.RecipeConvertMapper;
import com.example.cookingmanagement.mapper.RecipeMapper;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeMapper recipeMapper;
    private final RecipeConvertMapper recipeConvertMapper;
    private final IngredientMapper ingredientMapper;

    public RecipeService(RecipeMapper recipeMapper, RecipeConvertMapper recipeConvertMapper, IngredientMapper ingredientMapper) {
        this.recipeMapper = recipeMapper;
        this.recipeConvertMapper = recipeConvertMapper;
        this.ingredientMapper = ingredientMapper;
    }

    public List<Recipe> getAllRecipes() {
        return recipeMapper.findAll();
    }

    public Recipe getRecipeById(int id) {
        Recipe recipe = recipeMapper.findById(id);
        if (recipe == null) {
            return null; // 呼び出し元で null を確認する
        }

        recipe.setIngredients(ingredientMapper.findIngredientsByRecipeId(id));
        return recipe;
    }

    public void createRecipe(RecipeForm form) {
        Recipe recipe = recipeConvertMapper.toEntity(form);
        recipe.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        recipeMapper.insert(recipe); // IDがここで自動採番される

        int recipeId = recipe.getRecipeId(); // 自動採番されたIDを取得

        // 材料を保存
        for (IngredientForm ingredientForm : form.getIngredients()) {
            if (ingredientForm.getName() != null && !ingredientForm.getName().isBlank()) {
                ingredientMapper.insertIngredient(
                        ingredientForm.getName(),
                        ingredientForm.getQuantity(),
                        recipeId
                );
            }
        }
    }

    public void updateRecipe(int id, RecipeForm form) {
        Recipe recipe = recipeConvertMapper.toEntity(form);
        recipe.setRecipeId(id);
        recipe.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        // レシピ情報の更新
        recipeMapper.update(recipe);

        // 材料を一旦すべて削除してから、再挿入
        ingredientMapper.deleteByRecipeId(id);

        // 🔽 RecipeFormから材料を取得して保存
        if (form.getIngredients() != null) {
            for (IngredientForm ingredientForm : form.getIngredients()) {
                if (ingredientForm.getName() != null && !ingredientForm.getName().isBlank()) {
                    ingredientMapper.insertIngredient(
                            ingredientForm.getName(),
                            ingredientForm.getQuantity(),
                            id
                    );
                }
            }
        }
    }

    public RecipeForm convertToForm(Recipe recipe) {
        RecipeForm form = recipeConvertMapper.toForm(recipe);

        // レシピに紐づく材料を IngredientForm に変換してセット
        if (recipe.getIngredients() != null) {
            form.setIngredients(convertIngredientsToForm(recipe.getIngredients()));
        }

        return form;
    }

    public void deleteRecipe(int id) {
        // 材料を先に削除してからレシピを削除
        ingredientMapper.deleteByRecipeId(id);
        recipeMapper.deleteById(id);
    }

    public List<Recipe> searchByTitle(String keyword) {
        return recipeMapper.findByTitleLike("%" + keyword + "%");
    }

    private List<IngredientForm> convertIngredientsToForm(List<Ingredient> ingredients) {
        List<IngredientForm> forms = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            IngredientForm form = new IngredientForm();
            form.setName(ingredient.getName());
            form.setQuantity(ingredient.getQuantity());
            forms.add(form);
        }
        return forms;
    }
}
