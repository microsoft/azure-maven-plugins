/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.prompt;

import com.microsoft.azure.maven.spring.exception.NoResourcesAvailableException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

public interface IPrompter extends Closeable {

    /**
     * Promote user to input a text value through a terminal.
     *
     * @param name the property name
     * @param defaultValue the default value if user presses ENTER key.
     * @param regex the regex to check against user's input
     * @param isRequired whether or not a null/empty value is acceptable
     * @return the text value user has input
     * @throws IOException when there are any IO errors.
     */
    String promoteString(String name, String defaultValue, String regex, boolean isRequired) throws IOException;

    /**
     * Promote user to input a number value through a terminal.
     *
     * @param name the property name
     * @param defaultValue the default number value if user presses ENTER key.
     * @param minValue the minimal value
     * @param maxValue the maximal value
     * @param isRequired whether or not a null value is acceptable
     * @return the number value user has input
     * @throws IOException when there are any IO errors.
     */
    Integer promoteInteger(String name, Integer defaultValue, int minValue, int maxValue, boolean isRequired) throws IOException;


    /**
     * Promote user to choose YES or NO through a terminal. If this property is optional and there is no default option,
     * then this method returns null when user presses ENTER key.
     *
     * @param defaultValue the default option if user presses ENTER key.
     * @param message the promote message to give user a hint about the options.
     * @param isRequired whether or not user must to accept or decline explicitly.
     * @return the yes/no option
     * @throws IOException when there are any IO errors.
     */
    Boolean promoteYesNo(Boolean defaultValue, String message, boolean isRequired) throws IOException;

    /**
     * Promote user to choose some entities from a known list. if <code>allowEmpty</code> is true, then user has the option to select none from
     * the list, otherwise, at least one entry must be selected.
     *
     * @param <T> the entity type
     * @param name the property name
     * @param message the message print before printing the options
     * @param entities the known list which are to be selected in
     * @param getNameFunc the entity to string convert function
     * @param allowEmpty whether or not to accept empty list.
     * @param enterPromote the promote message to give user a hint about the behaviour of pressing ENTER key directly, should be align with the
     *  actual meaning of defaultValue
     * @param defaultEntities the default entities when pressing ENTER key directly.
     * @return the list user selected
     * @throws IOException when there are any IO errors.
     */
    <T> List<T> promoteMultipleEntities(String name, String message, List<T> entities, Function<T, String> getNameFunc, boolean allowEmpty, String enterPromote,
            List<T> defaultEntities) throws IOException;


    /**
     * Promote user to choose a single entity from a known list. if <code>isRequired</code> is true, then user must select one entity.
     *
     * @param <T> the entity type
     * @param name the property name
     * @param message the message print before printing the options
     * @param entities the known list which are to be selected in
     * @param getNameFunc the entity to string convert function
     * @param defaultEntity the default entity when pressing ENTER key directly.
     * @return the entity user selected
     * @throws IOException when there are any IO errors.
     */
    <T> T promoteSingleEntity(String name, String message, List<T> entities, T defaultEntity, Function<T, String> getNameFunc, boolean isRequired)
            throws IOException, NoResourcesAvailableException;
}
