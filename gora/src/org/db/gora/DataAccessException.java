/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.db.gora;

/**
 * Defines an exception that can occur while reading or writing database.
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */

public class DataAccessException extends Exception {
	public DataAccessException(String detailedMessage) {
		super(detailedMessage);
	}
	
	public DataAccessException(String detailedMessage, Throwable throwable) {
		super(detailedMessage, throwable);
	}

	private static final long serialVersionUID = -8507999681855900760L;
}
