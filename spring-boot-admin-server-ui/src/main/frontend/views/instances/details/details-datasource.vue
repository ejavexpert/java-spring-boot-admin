<!--
  - Copyright 2014-2018 the original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <sba-panel v-if="hasLoaded" :title="$t('instances.details.datasource.title', {dataSource: dataSource})">
    <div>
      <sba-alert v-if="error" :error="error" :title="$t('instances.details.datasource.fetch_failed')" />

      <div v-if="current" class="level datasource-current">
        <div class="level-item has-text-centered">
          <div>
            <p class="heading has-bullet has-bullet-info"
               v-text="$t('instances.details.datasource.active_connections')"
            />
            <p v-text="current.active" />
          </div>
        </div>
        <div class="level-item has-text-centered">
          <div>
            <p class="heading" v-text="$t('instances.details.datasource.min_connections')" />
            <p v-text="current.min" />
          </div>
        </div>
        <div class="level-item has-text-centered">
          <div>
            <p class="heading" v-text="$t('instances.details.datasource.max_connections')" />
            <p v-if="current.max >= 0" v-text="current.max" />
            <p v-else v-text="$t('instances.details.datasource.unlimited')" />
          </div>
        </div>
      </div>
      <datasource-chart v-if="chartData.length > 0" :data="chartData" />
    </div>
  </sba-panel>
</template>

<script>
import sbaConfig from '@/sba-config';
import subscribing from '@/mixins/subscribing';
import Instance from '@/services/instance';
import {concatMap, delay, retryWhen, timer} from '@/utils/rxjs';
import moment from 'moment';
import datasourceChart from './datasource-chart';
import {take} from 'rxjs/operators';

export default {
  props: {
    instance: {
      type: Instance,
      required: true
    },
    dataSource: {
      type: String,
      required: true
    }
  },
  mixins: [subscribing],
  components: {datasourceChart},
  data: () => ({
    hasLoaded: false,
    error: null,
    current: null,
    chartData: [],
  }),
  methods: {
    async fetchMetrics() {
      const responseActive = this.instance.fetchMetric('data.source.active.connections', {name: this.dataSource});
      const responseMin = this.instance.fetchMetric('data.source.min.connections', {name: this.dataSource});
      const responseMax = this.instance.fetchMetric('data.source.max.connections', {name: this.dataSource});

      return {
        active: (await responseActive).data.measurements[0].value,
        min: (await responseMin).data.measurements[0].value,
        max: (await responseMax).data.measurements[0].value
      };
    },
    createSubscription() {
      const vm = this;
      return timer(0, sbaConfig.uiSettings.pollTimer.datasource)
        .pipe(concatMap(vm.fetchMetrics), retryWhen(
          err => {
            return err.pipe(
              delay(1000),
              take(5)
            )
          }))
        .subscribe({
          next: data => {
            vm.hasLoaded = true;
            vm.current = data;
            vm.chartData.push({...data, timestamp: moment().valueOf()});
          },
          error: error => {
            vm.hasLoaded = true;
            console.warn(`Fetching datasource ${vm.dataSource} metrics failed:`, error);
            vm.error = error;
          }
        });
    }
  }
}
</script>

<style lang="scss">
.datasource-current {
  margin-bottom: 0 !important;
}
</style>
