/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Open Source Version is distributed with Sencha Ext JS pursuant to a FLOSS Exception agreed upon
 * between Sonatype, Inc. and Sencha Inc. Sencha Ext JS is licensed under GPL v3 and cannot be redistributed as part of a
 * closed source work.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
import {assign, createMachine} from "xstate";
import Axios from 'axios';
import {APIConstants} from '@sonatype/nexus-ui-plugin';

const {MALICIOUS_RISK_ON_DISK} = APIConstants.REST.PUBLIC;

export default createMachine({
  id: 'MaliciousRiskOnDiskMachine',
  initial: 'loading',

  context: {
    maliciousRiskOnDisk: {}
  },

  states: {
    loading: {
      invoke: {
        src: 'fetchData',
        onDone: {
          target: 'loaded',
          actions: ['setData']
        },
        onError: {
          target: 'loadError',
          actions: 'setError'
        }
      }
    },
    loaded: {},
    loadError: {
      on: {
        'RETRY': {
          target: 'loading',
          actions: 'clearError'
        }
      }
    }
  },
},
{
  actions: {
    setData: assign({
      maliciousRiskOnDisk: (_, event) => event.data?.data
    }),

    setError: assign({
      loadError: (_, event) => event.data.message
    }),

    clearError: assign({
      loadError: () => null
    })
  },
  services: {
    fetchData: () => Axios.get(MALICIOUS_RISK_ON_DISK)
  }
});