/*
 * Copyright 2014-2015 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.avanzabank.asterix.gs;

import org.openspaces.core.GigaSpace;

public interface GigaSpaceRegistry {
	
	/*
	 * TODO: om vi inför ett globalt register, hur ska vi då upptäcka när en GigaSpace-proxy inte används längre och därmed kan förstöras?
	 * 
	 * Det gäller även andra saker som slås upp över tjänste-bussen. Hur vet man att den uppslagna tjänsten inte används längre?
	 * 
	 * 
	 * Allt som slås upp över bussen måste kunna bindas om dynamiskt utan att konsumenten behöver slå upp tjänsten på nytt. Det kräver
	 * att den proxy som returneras abstraherar bort "återbindning". 
	 * 
	 * (O)relaterat: Hur vet man när det är dags att slå upp en remoting-tjänst mot service-bussen igen? Typiskt vill man väl göra en slagning
	 * när man börjar få "SpaceUnavailableException"? Skapa "special" proxy som tillåger att man lyssnar på att proxy:n blir nedkopplad?
	 * 
	 *  
	 * 
	 */
	
	
	/*
	 * Går det att designa ramverket så att GigaSpaceRegistry inte är en "core"-komponent i tjänstebussen? Vi vill dock att 
	 * GigaSpace instanser ska slås upp via tjänstebussen. Hur ska de som behöver tjänsteregistret få tag i en instans av det?
	 * 
	 *  
	 * 
	 * 
	 */
	
	GigaSpace lookup(String spaceName);

}
