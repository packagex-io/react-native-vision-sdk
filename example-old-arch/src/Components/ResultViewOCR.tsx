import React from 'react';
import {
  View,
  StyleSheet,
  TouchableOpacity,
  Modal,
  Text,
  ScrollView,
  Image,
} from 'react-native';
import Icon from 'react-native-vector-icons/Ionicons';

interface ResultViewOCRProps {
  visible: boolean;
  result: Result | null;
  setResult?: (result: Result | null) => void;
  mode?: string;
  onReportError?: (response: any) => void;
}

interface Result {
  [key: string]: any;
  image_url?: string; // Optional image URL for the result
}

const ResultViewOCR: React.FC<ResultViewOCRProps> = ({
  visible,
  result,
  setResult,
  mode,
  onReportError,
}) => {
  const renderItemView = (
    heading: string = '',
    value: string = '',
    key: string
  ) =>
    value ? (
      <View key={key} style={styles.itemTextContainer}>
        <Text style={styles.headingTextStyle}>
          {heading.replace(/\.|_/g, ' ')}:
        </Text>
        <Text style={styles.answerTextStyle}>{value}</Text>
      </View>
    ) : null;

  const renderHeadingItemView = (heading: string = '') => (
    <View style={styles.itemTextContainer}>
      <Text style={styles.subHeadingText}>{heading}</Text>
    </View>
  );

  const renderObject = (obj: Record<string, any>, prefix = '') => {
    return Object.entries(obj).map(([key, value]) => {
      if (typeof value === 'object' && value !== null) {
        // Recursive call for nested objects
        return renderObject(value, `${prefix}${key}.`);
      }
      // Handle non-object values and cast them to string
      return value
        ? renderItemView(`${prefix}${key}`, String(value), `${prefix}${key}`)
        : null;
    });
  };

  return (
    <Modal animationType="fade" transparent visible={visible}>
      <View style={styles.centeredViewModal}>
        <View style={styles.modalView}>
          <View style={styles.headerView}>
            <TouchableOpacity
              onPress={() => setResult && setResult(null)}
              style={styles.backIconContainer}
            >
              <Icon name="chevron-back-outline" size={40} color="white" />
            </TouchableOpacity>
            <View style={styles.headingTextContainer}>
              <Text style={{ color: 'yellow', fontSize: 20 }}>Scanned</Text>
            </View>
          </View>
          <ScrollView>
            {renderHeadingItemView('Result Details')}
            {result && renderObject(result)}

            {/* Display image if available */}
            {/* {result?.image_url ? (
              <View style={styles.imageContainer}>
                <Image
                  source={{ uri: result.image_url }}
                  style={styles.imageStyle}
                />
              </View>
            ) : null} */}
            <Image
              source={{
                uri: '/var/mobile/Containers/Data/Application/7631B0F9-C0FC-49A9-8458-6E6E2BCC5D54/Documents/VisionSDKOCRImages/4DD35637-150D-45A0-A1E9-6BA924378F3C',
              }}
              style={{ width: 200, height: 200 }}
            />
          </ScrollView>
        </View>
        {mode == 'on-device' ? (
          <TouchableOpacity
            onPress={() => onReportError && onReportError(result)}
          >
            <Text style={{ color: 'red', textAlign: 'center', marginTop: 20 }}>
              Report Error
            </Text>
          </TouchableOpacity>
        ) : null}
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  subHeadingText: {
    fontSize: 15,
    fontWeight: 'bold',
    color: 'orange',
  },
  itemTextContainer: {
    padding: 10,
  },
  headingTextStyle: {
    color: 'yellow',
    fontSize: 14,
    fontWeight: '400',
    paddingBottom: 5,
    textTransform: 'capitalize',
  },
  answerTextStyle: {
    color: 'white',
    fontSize: 12,
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
    paddingVertical: 10,
    flexDirection: 'row',
  },
  centeredViewModal: {
    flex: 1,
    // paddingTop: 20,
    justifyContent: 'center',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
  },
  modalView: {
    borderRadius: 12,
    paddingVertical: 40,
    paddingHorizontal: 10,
    height: '100%',
  },
  imageContainer: {
    alignItems: 'center',
    marginTop: 20,
  },
  imageStyle: {
    width: 200,
    height: 200,
    resizeMode: 'contain',
  },
});

export default ResultViewOCR;
