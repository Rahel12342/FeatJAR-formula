/*
 * Copyright (C) 2022 Sebastian Krieter, Elias Kuiter
 *
 * This file is part of formula.
 *
 * formula is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3.0 of the License,
 * or (at your option) any later version.
 *
 * formula is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with formula. If not, see <https://www.gnu.org/licenses/>.
 *
 * See <https://github.com/FeatureIDE/FeatJAR-formula> for further information.
 */
package de.featjar.formula.analysis.solver;

import de.featjar.formula.analysis.Analysis;

/**
 * Exception thrown when an {@link Analysis analysis} experiences a solver
 * timeout.<br>
 * Doesn't need to be caught explicitly.
 *
 * @author Sebastian Krieter
 */
public class RuntimeTimeoutException extends RuntimeException {

    public RuntimeTimeoutException() {
    }

    public RuntimeTimeoutException(String message) {
        super(message);
    }

    public RuntimeTimeoutException(Throwable cause) {
        super(cause);
    }

    public RuntimeTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
