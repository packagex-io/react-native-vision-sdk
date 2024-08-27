import React from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  Modal,
  Text,
  ScrollView,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';

function ResultView({ visible, result, setResult }: any) {
  const renderItemView = (heading = '', value = '') => {
    return (
      value && (
        <View style={styles.itemTextContainer}>
          <Text style={styles.headingTextStyle}>{heading}:</Text>
          <Text style={styles.answerTextStyle}>{value}</Text>
        </View>
      )
    );
  };
  const renderHeadingItemView = (heading = '') => {
    return (
      <View style={styles.itemTextContainer}>
        <Text style={styles.subHeadingText}>{heading}:</Text>
      </View>
    );
  };
  return (
    <Modal animationType="fade" transparent visible={visible}>
      <View style={styles.centeredViewModal}>
        <View style={styles.modalView}>
          <View style={styles.headerView}>
            <TouchableOpacity
              onPress={() => setResult('')}
              style={styles.backIconContainer}
            >
              <Icon name="chevron-back-outline" size={40} color="white" />
            </TouchableOpacity>
            <View style={styles.headingTextContainer}>
              <Text style={{ color: 'yellow', fontSize: 20 }}>Scanned</Text>
            </View>
          </View>
          <ScrollView>
            {renderItemView('Account ID', result?.account_id)}
            {renderItemView('Tracking #', result?.tracking_number)}
            {renderItemView('RMA #', result?.rma_number)}
            {renderItemView('Reference Number #', result?.reference_number)}
            {renderItemView('Courier', result?.provider_name)}
            {renderItemView('Weight', result?.weight)}
            {/* {result?.recipient?.name && ( */}
            <>
              {renderHeadingItemView('Receiver Info')}
              {renderItemView('Name', result?.recipient?.name)}
              {renderItemView('Business Name', result?.recipient?.business)}
              {renderItemView(
                'Street Address',
                result?.recipient?.address?.line1
              )}
              {renderItemView('City', result?.recipient?.address?.city)}
              {renderItemView('State', result?.recipient?.address?.state)}
              {renderItemView(
                'Zip Code',
                result?.recipient?.address?.postal_code
              )}
              {renderItemView(
                'Address',
                result?.recipient?.address?.formatted_address
              )}
            </>
            {/* )} */}
            {/* {result?.sender?.name && ( */}
            <>
              {renderHeadingItemView('Sender Info')}
              {renderItemView('Name', result?.sender?.name)}
              {renderItemView('Business Name', result?.sender?.business)}
              {renderItemView('Street Address', result?.sender?.address?.line1)}
              {renderItemView('City', result?.sender?.address?.city)}
              {renderItemView('State', result?.sender?.address?.state)}
              {renderItemView('Zip Code', result?.sender?.address?.postal_code)}
              {renderItemView(
                'Address',
                result?.sender?.address?.formatted_address
              )}
            </>
            {/* )} */}
            {renderItemView('Shipment Type', result?.type)}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}
const styles = StyleSheet.create({
  subHeadingText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: 'orange',
  },
  itemTextContainer: {
    padding: 10,
  },
  headingTextStyle: {
    color: 'yellow',
    fontSize: 16,
    fontWeight: '400',
    paddingBottom: 5,
  },
  answerTextStyle: {
    color: 'white',
    fontSize: 16,
    fontWeight: '400',
  },
  backIconContainer: {
    alignSelf: 'flex-start',
    alignItems: 'center',
  },
  headingTextContainer: {
    justifyContent: 'center',
    alignSelf: 'center',
    alignItems: 'center',
    width: '80%',
  },
  headerView: {
    flexDirection: 'row',
  },
  descriptionTextStyle: {
    color: 'white',
    marginTop: 15,
    letterSpacing: 2,
    textAlign: 'center',
    fontSize: 14,
    fontWeight: '500',
    left: 5,
    textAlignVertical: 'center',
  },
  centeredViewModal: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
  },
  modalView: {
    borderRadius: 12,
    paddingVertical: 40,
    paddingHorizontal: 10,
    height: '100%',
  },
});

export default ResultView;
