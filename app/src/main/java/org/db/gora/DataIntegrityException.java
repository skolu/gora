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
 * Defines an exception that occurs in case of schema errors.
 *
 * @author Sergey Kolupaev &lt;skolupaev@gmail.com&gt;
 */
public class DataIntegrityException extends Exception {
	
	public DataIntegrityException(String detailedMessage) {
		super(detailedMessage);
	}
	private static final long serialVersionUID = -3570674490101843350L;
}
